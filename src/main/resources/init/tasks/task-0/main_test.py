import importlib
import os
from distutils.errors import CompileError

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


def test_kopfzeile_Abgabe():
    """
    Checks whether the file exists or not and if it exists, checks
    also if the header has been populated with valid data.
    Till yet there's no check if the author and the id are existant
    in the given course.
    """
    file: str = 'Abgabe.txt'
    if not os.path.exists(file):
        raise FileNotFoundError(f'{file} is missing in submission.')
    if not check_header(file):
        raise ValueError('Missing or corrupt Data in header.')


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
        module = load('main')
    except Exception as e:
        raise CompileError('Compilation of C code failed.')
    assert module.printformula(4, 4, 6, 8) == 22
    assert 0.8405 > module.printformula(2.1e-1, 0.21, 0.021e1, 210e-3) > 0.8395

