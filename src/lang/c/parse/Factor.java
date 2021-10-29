package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;

public class Factor extends CParseRule {
	// factor ::= facterAmp | number
	private CParseRule number;
	private CParseRule factorAmp;
	public Factor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Number.isFirst(tk) || FactorAmp.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		if (FactorAmp.isFirst(tk)) {
			factorAmp = new FactorAmp(pcx);
			factorAmp.parse(pcx);
		} else {
			number = new Number(pcx);
			number.parse(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (number != null) {
			number.semanticCheck(pcx);
			setCType(number.getCType());		// number の型をそのままコピー
			setConstant(number.isConstant());	// number は常に定数
		} else if (factorAmp != null) {
			factorAmp.semanticCheck(pcx);
			setCType(factorAmp.getCType());
			setConstant(factorAmp.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; factor starts");
		if (factorAmp != null) { factorAmp.codeGen(pcx); }
		if (number != null) { number.codeGen(pcx); }
		o.println(";;; factor completes");
	}
}