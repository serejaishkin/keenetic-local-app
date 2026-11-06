# -*- coding: utf-8 -*-
from ghidra.app.decompiler import DecompInterface
from ghidra.util.task import ConsoleTaskMonitor
fm = currentProgram.getFunctionManager()
ifc = DecompInterface(); ifc.openProgram(currentProgram); mon = ConsoleTaskMonitor()
out = open("/tmp/tok6.txt", "w")
sel = []
for f in fm.getFunctions(True):
    full = f.getName(True)
    if "Token" in full and ("Command" in full or "Credentials" in full or "Authenticat" in full):
        sel.append(f)
out.write("%d functions\n" % len(sel))
for f in sel: out.write("%s %s\n" % (f.getEntryPoint(), f.getName(True)[:150]))
out.write("\n")
want = [f for f in sel if any(k in f.getName(True) for k in ("Generate", "Delete", "Execute", "GetArgumentParser", "TokensHolder::Add", "Create", "Add"))]
for f in want[:14]:
    r = ifc.decompileFunction(f, 120, mon)
    if r.decompileCompleted():
        out.write("==== %s @%s\n%s\n" % (f.getName(True)[:120], f.getEntryPoint(), r.getDecompiledFunction().getC()[:5000]))
out.close()
