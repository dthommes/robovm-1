#!/bin/bash
set -e
BASE=$(cd $(dirname $0); pwd -P)
JAVAOUT="$BASE/src/main/java/org/robovm/llvm/binding"
COUT="$BASE/src/main/native"

mkdir -p "$JAVAOUT"
mkdir -p "$COUT"
swig -includeall -I"$BASE/src/main/swig/include-stub" -I"$BASE/src/main/swig/include" -outdir "$JAVAOUT" -o "$COUT"/LLVM_wrap.c -java -package org.robovm.llvm.binding -fakeversion 2.0.4 "$BASE/src/main/swig/LLVM.i"
# fix permission
chmod -R 644 "$JAVAOUT/"
chmod 744 "$JAVAOUT"
chmod 644 "$COUT"/LLVM_wrap.c
