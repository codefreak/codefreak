/*******************************************************************************
* Projekt: ProgrammierenInC
* Dateiname: main.c
* Beschreibung: ???
* Autor: Max Mustermann
* Matr.-Nr.: XXXXXX
* Erstellt: XX.XX.2020
*******************************************************************************/

#include <stdio.h>
#include <stdlib.h>

void flushIn() {
    // Bitte zum 'entleeren' des Streams nutzen
    int c;
    do {
        c = getchar();
    } while (c != '#' && c != '\n' && c != EOF);
}

int main(void) {

    // Lösung hier

    printf("Name: ??? \n", ???);
    printf("Geburtsdatum: ??? \n", ???);
    printf("Steuersatz: ??? \n", ???);

    return 0;
}

