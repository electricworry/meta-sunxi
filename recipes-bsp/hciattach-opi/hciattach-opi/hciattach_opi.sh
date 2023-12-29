#!/bin/bash

rfkill unblock all
/usr/sbin/hciattach_opi -n -s 1500000 /dev/ttyBT0 sprd &
