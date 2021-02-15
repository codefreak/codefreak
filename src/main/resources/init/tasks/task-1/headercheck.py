from datetime import datetime
from typing import List


labels: dict = {
    'autor': 'Autor:',
    'matnr': 'Matr.-Nr.:',
    'date': 'Erstellt:'
}


def check_header(filename) -> bool:
    with open(filename) as file:
        content: List[str] = file.readlines()
        header_rows: List[str] = []
        header_started: bool = False
        for line in content:
            if '/*' in line:
                header_started = True
            if header_started:
                header_rows.append(line)
            if '*/' in line:
                break

        if len(header_rows) < 2:
            return False

        try:
            a, b, c = False, False, False
            for line in header_rows:
                if labels['autor'] in line:
                    autor: str = str(line.split(labels['autor'])[-1]).strip()
                    print(f'Found Author: {autor}')
                    a = True
                if labels['matnr'] in line:
                    matnr: int = int(str(
                        line.split(labels['matnr'])[-1]
                    ).strip())
                    if 800000 < matnr < 1000000:
                        print(f'Found Matr.Nr.: {matnr}')
                        b = True
                if labels['date'] in line:
                    date: datetime = datetime.strptime(
                        str(line.split(labels['date'])[-1]).strip(),
                        "%d.%m.%Y"
                    )
                    print(f'Found Date: {date}')
                    c = True

            return a and b and c
        except Exception as e:
            return False

