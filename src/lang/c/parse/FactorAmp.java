package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class FactorAmp extends CParseRule{
	// factor ::= facterAmp | number
	private CToken amp;
	private CParseRule right;

	public FactorAmp(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_ADDRESS;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		amp = ct.getCurrentToken(pcx);
		// &の次の字句を読む
		CToken tk = ct.getNextToken(pcx);
		if (Number.isFirst(tk)) {
			right = new Number(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "&の後ろはnumberです");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (right != null) {
			right.semanticCheck(pcx);
			int rt = right.getCType().getType(); // &右側の型
			if (rt == CType.T_err) {
				pcx.fatalError(amp.toExplainString() + "側の型[" + right.getCType().toString() + "はアドレスにできません");
			}
			setCType(CType.getCType(CType.T_pint));
			setConstant(right.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (right != null) {
			right.codeGen(pcx); // 右側のコード生成を依頼
		}
	}
}