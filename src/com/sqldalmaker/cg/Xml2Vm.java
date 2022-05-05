/*
    Copyright 2011-2022 sqldalmaker@gmail.com
    SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
    Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.cg;

import com.sqldalmaker.jaxb.settings.*;

import java.io.StringWriter;
import java.util.List;

public class Xml2Vm {

	public static String parse(VmXml vm_xml) {
		StringWriter sw = new StringWriter();
		List<Object> interExpr = vm_xml.getIfOrSetOrForeach();
		processStatements(interExpr, sw);
		return sw.toString();
	}

	private static void processStatement(Object expr, StringWriter sw) {
		if (expr instanceof If) {
			If ex = (If) expr;
			sw.append("#if(");
			sw.append(ex.getVar());
			sw.append(")");
			List<Object> interExpr = ex.getIfOrSetOrForeach();
			processStatements(interExpr, sw);
			List<RootStatements> elseifAndElse = ex.getElseifAndElse();
			processIf(elseifAndElse, sw);
			sw.append("#end");
		} else if (expr instanceof Foreach) {
			Foreach ex = (Foreach) expr;
			sw.append("#foreach(");
			sw.append(ex.getVar());
			sw.append(" in ");
			sw.append(ex.getIn());
			sw.append(")");
			List<Object> interExpr = ex.getIfOrSetOrForeach();
			processStatements(interExpr, sw);
			sw.append("#end");
		} else if (expr instanceof Macro) {
			Macro ex = (Macro) expr;
			sw.append("#macro(");
			sw.append(ex.getName());
			sw.append(" in ");
			sw.append(ex.getParams());
			sw.append(")");
			List<Object> interExpr = ex.getIfOrSetOrForeach();
			processStatements(interExpr, sw);
			sw.append("#end");
		} else if (expr instanceof Set) {
			Set ex = (Set) expr;
			sw.append("#set(");
			sw.append(ex.getVar());
			sw.append(" = ");
			sw.append(ex.getValue());
			sw.append(")");
		} else if (expr instanceof Print) {
			Print ex = (Print) expr;
			String value = ex.getVar();
			if (value != null) {
				sw.append(value);
			}
		} else {
//			System.out.print(expr.getClass());
			sw.append("\n");
		}
	}

	private static void processStatements(List<Object> epressions, StringWriter sw) {
		// boolean res = false;
		for (Object expr : epressions) {
			processStatement(expr, sw);
		}
	}

	private static void processIf(List<RootStatements> epressions, StringWriter sw) {
		for (Object expr : epressions) {
			if (expr instanceof Elseif) {
				Elseif ex = (Elseif) expr;
				sw.append("#elseif(");
				sw.append(ex.getVar());
				sw.append(")");
				List<Object> interExpr = ex.getIfOrSetOrForeach();
				processStatements(interExpr, sw);
			} else if (expr instanceof Else) {
				Else ex = (Else) expr;
				sw.append("#else");
				List<Object> interExpr = ex.getIfOrSetOrForeach();
				processStatements(interExpr, sw);
			} else {
				processStatement(expr, sw);
			}
		}
	}
}
