#! /bin/bash

lsof -a -i4 -i6 -itcp | grep :3000 | grep ESTABLISHED | wc -l
