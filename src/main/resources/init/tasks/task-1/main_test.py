import importlib
import os
from distutils.errors import CompileError
import cffi
from headercheck import check_header

template = {
    'int i, j;': False,
    'float a = 2.3;': False,
    'int grösse = 5;': False,
    "char a = 'x';": True,
    'float f = 2.7e-5;': False,
    "const float Zinssatz;": False,
    "char b = 'ab';": False,
    "char c = '\'';": True,
    'const int n = 3;': True,
    'int grösse;': False,
    "char x = 'Y';": True,
    "const char z = '\\';": True,
    'double umsatz-firma;': False,
    'int m;': False,
    'm = 2.0;': False,
    'n = 5;': False,
    'Zinssatz = 1.5;': False,
    'f * f = f * 5;': False,
    "printf('Hallo Welt\n');": False,
    'return 0;': True
}


def load(filename):
    # template name for the modified c file
    pseudo = f'{filename}_'

    # source and header file inclusion
    source = open(filename + '.c').read()
    includes = open(filename + '.h').read()

    # remove old build files, due to access errors
    for file in os.listdir():
        # depending on system setup, compiler adds python specific information
        # to the library e.g: main_.cp37-win_amd64.pyd
        if file.startswith(pseudo) and file.endswith('.pyd'):
            os.remove(file)

    # build python module from c files
    builder = cffi.FFI()
    builder.cdef(includes)
    builder.set_source(pseudo, source)
    builder.compile()

    # search build file and ensure correct spelling without system
    # dependent build names as mentioned above
    for file in os.listdir():
        if file.startswith(pseudo) and file.endswith('.pyd'):
            os.rename(file, f'{pseudo}.pyd')

    # load converted module
    module = importlib.import_module(pseudo).lib
    return module


def excluded(line: str) -> bool:
    line = line.lstrip()
    return line.startswith('//')


def write_line_2_log(line: str):
    dir: str = 'test-results'
    if not os.path.exists(dir):
        os.mkdir(dir)
    with open(os.path.join(dir, 'lines.txt'), 'a') as logfile:
        logfile.write(line)


def test_kopfzeile_main_C():
    """
    Checks whether the file exists or not and if it exists, checks
    also if the header has been populated with valid data.
    Till yet there's no check if the author and the id are existant
    in the given course.
    """
    file: str = 'main.c'
    if not os.path.exists(file):
        raise FileNotFoundError(f'{file} is missing in submission.')
    if not check_header(file):
        raise ValueError('Missing or corrupt Data in header.')


def test_can_compile():
    """
    Checks whether the 'main.c' can be compiled or not.
    """
    try:
        module = load('main')
    except Exception as e:
        raise CompileError('Compilation of C code failed.')
    assert module.run() == 0


def test_each_line():
    """
        Iterate over all lines of the method 'int run()'
        and compare line with template defined value, if it is valid or not.
        Results are counted for lines which should be excluded (not_excluded) by
        the student as well as them that are excluded (false_excluded) although
        they are valid.
        """
    with open('main.c') as cfile:
        content = cfile.readlines()
        begin = False
        false_excluded: int = 0
        not_excluded: int = 0
        for line in content:
            if begin:
                write_line_2_log(line)
                for key in template:
                    if key in line and template[key] == excluded(line):
                        print(f'{key} -> {line}')
                        print(f'\t{template[key]} -> {excluded(line)}')
                        if template[key]:
                            false_excluded += 1
                        else:
                            not_excluded += 1
                        break
            if 'int run()' in line:
                begin = True
            if 'return 0;' in line:
                break
    if false_excluded > 0 or not_excluded > 0:
        raise ValueError(
            f'Not all issues have been fixed correctly.\n'
            f'{false_excluded} are false excluded.\n'
            f'{not_excluded} are still left to exclude.'
        )

