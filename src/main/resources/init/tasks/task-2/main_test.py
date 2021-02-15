import importlib
import os
from datetime import datetime
from distutils.errors import CompileError
import platform

import cffi
from headercheck import check_header


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


def getArgs():
    # reading args.txt to get values for comparing
    if not os.path.exists('args.txt'):
        raise FileNotFoundError('Argument file for execution is not provided.')
    content = open('args.txt').readlines()
    content = [str(x).replace('#', '').replace('\n', '') for x in content]

    name = f'{(content[0][0]).capitalize()}.{str(content[1])[:8]}'
    date = datetime.strptime(content[2].strip(), '%d.%m.%Y')
    tax = float(content[3])

    return name, date, tax


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


def test_function():
    try:
        os.system('gcc main.c -o runner')
    except Exception as e:
        raise CompileError('Compilation of C code failed.')
    logfile: str = 'test-results/result.txt'
    try:
        if not os.path.exists('test-results'):
            os.mkdir('test-results')
        # writing output of run to result.txt
        if platform.system() == 'Linux':
            os.system(f'cat args.txt | ./runner > {logfile}')
        else:
            os.system(f'type args.txt | runner > {logfile}')
    except Exception as e:
        raise RuntimeError(f'Running main.c failed.')
    name, date, tax = getArgs()
    n, d, t = False, False, False
    if os.path.exists(logfile):
        content = open(logfile).readlines()
        for line in content:
            if 'name:' in line.lower():
                print(name, '->', str(line.split(':')[-1]).strip().replace(" ", ""))
                n = name == str(line.split(':')[-1]).strip().replace(" ", "")
            if 'geburtsdatum:' in line.lower():
                print(date, '->', datetime.strptime(
                    str(line.split(':')[-1]).strip(),
                    '%d.%m.%Y'
                ))
                d = date == datetime.strptime(
                    str(line.split(':')[-1]).strip(),
                    '%d.%m.%Y'
                )
            if 'steuersatz:' in line.lower():
                print(tax, '->', float(str(line.split(':')[-1]).strip()))
                t = tax == float(str(line.split(':')[-1]).strip())
        if not (t and d and n):
            print(f'Tax: {t}, Name: {n}, Date: {d}')
            raise RuntimeError(f'Values have not been handled successful. Tax: {t}, Name: {n}, Date: {d}')
    else:
        raise RuntimeError(f'Running main.c failed.')


if __name__ == '__main__':
    test_function()