package amstin.tools;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import amstin.models.grammar.Alt;
import amstin.models.grammar.Bool;
import amstin.models.grammar.Element;
import amstin.models.grammar.Grammar;
import amstin.models.grammar.Id;
import amstin.models.grammar.Int;
import amstin.models.grammar.Iter;
import amstin.models.grammar.IterSep;
import amstin.models.grammar.IterSepStar;
import amstin.models.grammar.IterStar;
import amstin.models.grammar.Key;
import amstin.models.grammar.Lit;
import amstin.models.grammar.Opt;
import amstin.models.grammar.Real;
import amstin.models.grammar.Ref;
import amstin.models.grammar.Rule;
import amstin.models.grammar.Str;
import amstin.models.grammar.Sym;
import amstin.models.grammar.Symbol;

@SuppressWarnings("unchecked")
public class ModelToString {

	public static void unparse(Grammar grammar, Object obj, Writer writer) {
		ModelToString unp = new ModelToString(grammar, obj, writer);
		try {
			unp.unparse();
		} 
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Grammar grammar;
	private Object root;
	private IdentityHashMap<Object, String> labels;
	private Writer writer;
	private Set<String> reserved;

	private ModelToString(Grammar grammar, Object obj, Writer writer) {
		this.grammar = grammar;
		this.root = obj;
		this.labels = new IdentityHashMap<Object, String>();
		this.writer = writer;
		this.reserved = grammar.reservedKeywords();
	}
	
	private void unparse() throws IOException {
		Symbol sym = inferSymbol(root);
		collectRec(sym, root, null);
		unparseRec(sym, root);
	}
	
	private Symbol inferSymbol(Object obj) {
		for (Rule rule: grammar.rules) {
			if (findAlt(rule, obj) != null) {
				Sym sym = new Sym();
				sym.rule = rule;
				return sym;
			}
		}
		return null;
	}

	private void collectRec(Symbol sym, Object obj, String label) {
		if (sym instanceof Opt && obj == null) {
			return;
		}
		else if (sym instanceof Opt) {
			collectRec(((Opt)sym).arg, obj, label);
		}
		else if (obj instanceof List) {
			collectInList(sym, (List) obj, label);
		}
		else if (sym instanceof Sym){
			collectInCons(((Sym)sym).rule, obj, label);
		}

	}
	
	private void collectInCons(Rule rule, Object obj, String label) {
		Alt alt = findAlt(rule, obj); 
		
		// Scan for a key element, and use that as the label of the
		// current obj
		for (int i = 0; i < alt.elements.size(); i++) {
			Element elt = alt.elements.get(i);
			Symbol sym = elt.symbol;
			if (elt.label == null) {
				continue;
			}
			if (sym instanceof Key) {
				String fieldName = elt.label.name;
				Class<?> klz = obj.getClass();
				try {
					Field f = klz.getField(fieldName);
					String myLabel = (String)f.get(obj);
					label = label == null ? myLabel : label + "." + myLabel ;
					labels.put(obj, label);
					break;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		for (int i = 0; i < alt.elements.size(); i++) {
			Element elt = alt.elements.get(i);
			Symbol sym = elt.symbol;
			if (elt.label == null) {
				continue;
			}
			String fieldName = elt.label.name;
			Class<?> klz = obj.getClass();
			try {
				Field f = klz.getField(fieldName);
				Object value = f.get(obj);
				collectRec(sym, value, label);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void collectInList(Symbol sym, List l, String label) {
		Symbol arg = null;
		if (sym instanceof Iter) {
			arg = ((Iter)sym).arg;
		}
		if (sym instanceof IterStar) {
			arg = ((IterStar)sym).arg;
		}

		if (sym instanceof IterSep) {
			arg = ((IterSep)sym).arg;
		}
		if (sym instanceof IterSepStar) {
			arg = ((IterSepStar)sym).arg;
		}
		
		if (arg == null) {
			throw new RuntimeException("Invalid symbol in collectInList; expected iter or iterStar (or sep variants), got: " + sym);
		}
		
		for (Object o: l) {
			collectRec(arg, o, label);
		}		
	}
	
	private void unparseRec(Symbol sym, Object obj) throws IOException {
		if (sym instanceof Lit) {
			writer.write(((Lit)sym).value);
		}
		else if (sym instanceof Ref && labels.containsKey(obj)) {
			writer.write(labels.get(obj));
		}
		else if (sym instanceof Key && obj instanceof String) {
			writer.write((String)obj);
		}
		else if (sym instanceof Id && obj instanceof String) {
			if (reserved.contains(obj)) {
				writer.write("\\" + obj);
			}
			else {
				writer.write((String)obj);
			}
		}
		else if (sym instanceof Str && obj instanceof String) {
			writer.write(unparseString((String)obj));
		}
		else if (sym instanceof Int && obj instanceof Integer) {
			writer.write(obj.toString());
		}
		else if (sym instanceof Real && obj instanceof Double) {
			writer.write(obj.toString());
		}
		else if (sym instanceof Bool && obj instanceof Boolean) {
			writer.write(obj.toString());
		}
		else if (sym instanceof Opt && ((Opt)sym).arg instanceof Lit && obj instanceof Boolean) {
			Lit lit = (Lit) ((Opt)sym).arg;
			writer.write(obj.equals(true) ? lit.value : "");
		}
		else if (sym instanceof Opt && obj == null) {
			return;
		}
		else if (sym instanceof Opt) {
			unparseRec(((Opt)sym).arg, obj);
		}
		else if (obj instanceof List) {
			List l = (List)obj;
			unparseList(sym, l);
		}
		else if (sym instanceof Sym) {
			unparseUsingAlt(((Sym)sym).rule, obj);
		}
		else {
			throw new RuntimeException("Unhandled symbol object combination: " + sym + "; obj = " + obj);
		}
		
	}

	private String unparseString(String str) {
		return "\"" + str
				.replaceAll("\\n", "\\n")
				.replaceAll("\\r", "\\n")
				.replaceAll("\\t", "\\n")
				.replaceAll("\\f", "\\n")
				.replaceAll("\\\\", "\\\\")
				.replaceAll("\\\"", "\\\"") + "\"";
	}

	private void unparseList(Symbol sym, List l) throws IOException {
		Symbol arg = null;
		String sep = "";
		
		if (sym instanceof Iter) {
			arg = ((Iter)sym).arg;
		}
		if (sym instanceof IterStar) {
			arg = ((IterStar)sym).arg;
		}

		if (sym instanceof IterSep) {
			arg = ((IterSep)sym).arg;
			sep = ((IterSep)sym).sep;
		}
		if (sym instanceof IterSepStar) {
			arg = ((IterSepStar)sym).arg;
			sep = ((IterSepStar)sym).sep;
		}

		if (arg == null) {
			throw new RuntimeException("Invalid symbol in unparseList; expected aiter or iterStar (or sep variants), got: " + sym);
		}
		
		for (int i = 0; i < l.size(); i++) {
			unparseRec(arg, l.get(i));
			if (i < l.size() - 1) {
				writer.write(sep);
				space();
			}
		}
	}

	private void unparseUsingAlt(Rule rule, Object obj) throws IOException {
		Alt alt = findAlt(rule, obj); 
				
		for (int i = 0; i < alt.elements.size(); i++) {
			Element elt = alt.elements.get(i);
			Symbol sym = elt.symbol;
			if (elt.label == null) {
				// must be literal, so null is fine.
				unparseRec(sym, null);
			}
			else {
				Class<?> klz = obj.getClass();
				
				String fieldName = elt.label.name;
				try {
					Field f = klz.getField(fieldName);
					Object value = f.get(obj);
					unparseRec(sym, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			if (i < alt.elements.size() - 1) {
				space();
			}
		}
	}

	private void space() throws IOException {
		writer.write(" ");
	}

	private Alt findAlt(Rule rule, Object obj) {
		if (obj == null) {
			// find the empty alternative
			for (Alt alt: rule.alts) {
				if (alt.elements.isEmpty()) {
					return alt;
				}
			}
			throw new RuntimeException("No empty alternative found in rule: " + rule.name);
		}


		if (!rule.isInjection()) {
			String name = obj.getClass().getSimpleName();
			for (Alt alt: rule.alts) {
				if (alt.type != null && alt.type.name.equals(name)) {
					return alt;
				}
			}
			// if no alt found, use rulename and first alt.
			if (rule.name.equals(name)) {
				return rule.alts.get(0);
			}
		}

		// if injection look over them
		if (rule.isInjection()) {
			for (Alt alt: rule.alts) {
				Sym sym = (Sym) alt.elements.get(0).symbol;
				Alt result = findAlt(sym.rule, obj);
				if (result != null) {
					return result;
				}
			}
		}
		
		return null;
	}
	
	
}
