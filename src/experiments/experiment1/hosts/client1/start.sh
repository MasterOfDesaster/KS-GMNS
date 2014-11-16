#!/bin/bash

# Hier Pfad zu den Java-class-files eintragen
export PATH_TO_CLASSFILES="../../../../../out/production/praktikum"

groovy -cp $PATH_TO_CLASSFILES":../../../../../libs/jpcap.jar" -D stand.alone Client.groovy

