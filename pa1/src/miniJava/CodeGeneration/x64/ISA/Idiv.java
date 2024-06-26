package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.RS;
import miniJava.CodeGeneration.x64.x64;

public class Idiv extends Instruction {
	
	// Solve: RDX:RAX / rm
	// RAX:= quotient
	// RDX:= remainder
	public Idiv(RS modrmsib) {
		opcodeBytes.write(0xF7);
		modrmsib.SetRegR(x64.mod543ToReg(7));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
}
