package lang.c;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import lang.Tokenizer;

public class CTokenizer extends Tokenizer<CToken, CParseContext> {
	@SuppressWarnings("unused")
	private CTokenRule	rule;
	private int			lineNo, colNo;
	private char		backCh;
	private boolean		backChExist = false;

	public CTokenizer(CTokenRule rule) {
		this.rule = rule;
		lineNo = 1; colNo = 1;
	}

	private InputStream in;
	private PrintStream err;

	private char readChar() {
		char ch;
		if (backChExist) {
			ch = backCh;
			backChExist = false;
		} else {
			try {
				ch = (char) in.read();
			} catch (IOException e) {
				e.printStackTrace(err);
				ch = (char) -1;
			}
		}
		++colNo;
		if (ch == '\n')  { colNo = 1; ++lineNo; }
//		System.out.print("'"+ch+"'("+(int)ch+")");
		return ch;
	}
	private void backChar(char c) {
		backCh = c;
		backChExist = true;
		--colNo;
		if (c == '\n') { --lineNo; }
	}

	// 現在読み込まれているトークンを返す
	private CToken currentTk = null;
	public CToken getCurrentToken(CParseContext pctx) {
		return currentTk;
	}
	// 次のトークンを読んで返す
	public CToken getNextToken(CParseContext pctx) {
		in = pctx.getIOContext().getInStream();
		err = pctx.getIOContext().getErrStream();
		currentTk = readToken();
//		System.out.println("Token='" + currentTk.toString());
		return currentTk;
	}
	private CToken readToken() {
		CToken tk = null;
		char ch;
		int  startCol = colNo;
		StringBuffer text = new StringBuffer();

		int state = 0;
		boolean accept = false;
		while (!accept) {
			switch (state) {
			case 0:					// 初期状態
				ch = readChar();
				if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
				} else if (ch == (char) -1) {	// EOF
					startCol = colNo - 1;
					state = 1;
				} else if (ch >= '1' && ch <= '9') { // 1~9
					startCol = colNo - 1;
					text.append(ch);
					state = 3;
				} else if (ch == '0') { // 0
					startCol = colNo - 1;
					text.append(ch);
					state = 10;
				} else if (ch == '+') {  // プラス
					startCol = colNo - 1;
					text.append(ch);
					state = 4;
				} else if (ch == '-') { // マイナス
					startCol = colNo - 1;
					text.append(ch);
					state = 8;
				} else if (ch == '/') { // スラッシュ
					startCol = colNo - 1;
					text.append(ch);
					state = 5;
				} else if (ch == '&') { // &
					startCol = colNo - 1;
					text.append(ch);
					state = 9;
				} else {			// ヘンな文字を読んだ
					startCol = colNo - 1;
					text.append(ch);
					state = 2;
				}
				break;
			case 1:					// EOFを読んだ
				tk = new CToken(CToken.TK_EOF, lineNo, startCol, "end_of_file");
				accept = true;
				break;
			case 2:					// ヘンな文字を読んだ
				tk = new CToken(CToken.TK_ILL, lineNo, startCol, text.toString());
				accept = true;
				break;
			case 3:					// 数（10進数）の開始
				ch = readChar();
				if (Character.isDigit(ch)) {
					text.append(ch);
				} else {
					if (Integer.decode(text.toString()).intValue() > 32767) { // オーバーフロー
						backChar(ch);
						state = 2;
					} else {
						// 数の終わり
						backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
						tk = new CToken(CToken.TK_NUM, lineNo, startCol, text.toString());
						accept = true;
					}
				}
				break;
			case 4:					// +を読んだ
				tk = new CToken(CToken.TK_PLUS, lineNo, startCol, "+");
				accept = true;
				break;
			case 8:					// -を読んだ
				tk = new CToken(CToken.TK_MINUS, lineNo, startCol, "-");
				accept = true;
				break;
			case 5: // /を読んだ
				ch = readChar();
				if (ch == '/') {
					text.append(ch);
					state = 6;
				} else if (ch == '*') {
					text.append(ch);
					state = 7;
				} else {
					text.append(ch);
					state = 2;
				}
				break;
			case 6: // "//"で始まるコメントを読んだ
				ch = readChar();
				if (ch == '\r' || ch == (char) - 1) {
					text = new StringBuffer();
					state = 0;
				} else {
					text.append(ch);
				}
				break;
			case 7: // "/*"で始まるコメントを読んだ
				ch = readChar();
				if (ch == '*') {
					text.append(ch);
					ch = readChar();
					if (ch == '/') {
						text = new StringBuffer();
						state = 0;
					} else {
						backChar(ch);
					}
				} else if (ch == (char) - 1) {
					state = 2;
				} else {
					text.append(ch);
				}
				break;
			case 9: // アドレス
				ch = readChar();
				if (Character.isDigit(ch)) {
					text.append(ch);
				} else {
					// アドレスの終わり
					backChar(ch);	// アドレスを表さない文字は戻す（読まなかったことにする）
					tk = new CToken(CToken.TK_ADDRESS, lineNo, startCol, text.toString());
					accept = true;
				}
				break;
			case 10: // 10進数 8進数 16進数
				ch = readChar();
				
				if (ch >= '0' && ch <= '7') { // 8進数(正常)
					text.append(ch);
					state = 13;
				} else if (ch == '8' || ch == '9') { // 8進数(異常)
					text.append(ch);
					state = 14;
				} else if (ch == 'x' || ch == 'X') { // 16進数(正常)
					text.append(ch);
					state = 11;
				} else { // 10進数(0)
					// 数の終わり
					backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
					tk = new CToken(CToken.TK_NUM, lineNo, startCol, text.toString());
					accept = true;
				}
				break;
			case 13: // 8進数(正常)
				ch = readChar();
				if (ch >= '0' && ch <= '7') { // 8進数(正常)
					text.append(ch);
					state = 13;
				} else if (ch == '8' || ch == '9') { // 8進数(異常)
					text.append(ch);
					state = 14;
				} else { // 8進数の終了
					// 数値がオーバーフロー
					if (text.length() >= 8 || (text.length() == 7 && text.charAt(1) != '0' && text.charAt(1) != '1')) {
						backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
						state = 2;
					} else {
						// 数の終わり
						backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
						tk = new CToken(CToken.TK_NUM, lineNo, startCol, text.toString());
						accept = true;
					}
				}
				break;
			case 14: // 8進数(異常)
				ch = readChar();
				if (Character.isDigit(ch)) {
					text.append(ch);
				} else {
					backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
					state = 2;
				}
				break;
			case 11: // 16進数かも
				ch = readChar();
				
				if (Character.isDigit(ch) ||
						(ch >= 'a' && ch <= 'f') ||
						(ch >= 'A' && ch <= 'F')) {
					text.append(ch);
					state = 12;
				} else {
					backChar(ch);
					state = 2;
				}
				break;
			case 12: // 16進数(正常)
				ch = readChar();
				if (Character.isDigit(ch) ||
						(ch >= 'a' && ch <= 'f') ||
						(ch >= 'A' && ch <= 'F')) {
					text.append(ch);
				} else {
					if (text.length() >= 7) {
						backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
						state = 2;
					} else {
						backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
						tk = new CToken(CToken.TK_NUM, lineNo, startCol, text.toString());
						accept = true;
					}
				}
				break;
			}
		}
		return tk;
	}
}
