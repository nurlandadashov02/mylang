# mylang
Implementation of a translator called mylang2IR for a language called MyLang.

The given problem requires us to develop a translator called
mylang2IR for a language called MyLang. The main purpose is to
read input file.my and produce LLVM (low-level virtual machine)
IR(intermediate representation) code in file.ll. Therefore, the
program should not evaluate the given statements in the input
file but should just create an LLVM IR, so that LLVM could
correctly output the results. We had to consider the assignment,
if, while, print, choose statements, and in these statements,
there might be expressions with arithmetic operations, and the
correct precedence should be followed.
