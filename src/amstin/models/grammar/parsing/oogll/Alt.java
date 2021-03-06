package amstin.models.grammar.parsing.oogll;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amstin.models.grammar.parsing.oogll.sppf.Node;
import amstin.models.grammar.parsing.oogll.symbol.NonTerminal;
import amstin.models.grammar.parsing.oogll.symbol.Symbol;
import amstin.models.grammar.parsing.oogll.symbol.Terminal;


public class Alt implements IParser {
	private final List<Symbol> elts;
	private NonTerminal nonTerminal;
	private final Item[] items;
	private final Set<Alt>[] disallowed;
	private final int arity;

	@SuppressWarnings("unchecked")
	public Alt(Symbol ...elts) {
		this.elts = Arrays.asList(elts);
		disallowed = new Set[elts.length];
		items = new Item[elts.length + 1];
		for (int i = 0; i < elts.length; i++) {
			items[i] = new Item(this, i);
		}
		items[elts.length] = new Item(this, elts.length);
		this.arity = elts.length;
	}

	public int arity() {
		return arity;
	}
	
	public Symbol get(int i) {
		return elts.get(i);
	}
	
	public void disallowAt(int i, Alt alt) {
		if (disallowed[i] == null) {
			disallowed[i] = new HashSet<Alt>();
		}
		disallowed[i].add(alt);
	}

	public void leftAssociative() {
		disallowAt(arity() - 1, this);
	}

	public void rightAssociative() {
		disallowAt(0, this);
	}

	public void nonAssociative() {
		leftAssociative();
		rightAssociative();
	}
	
	public Set<Alt>[] getDisallowed() {
		return disallowed;
	}
	
	public void strongerThan(Alt alt) {
		// (-x).strongerThan(x*x)
		// x*x
		// - x > [x * x] > x + x
		for (int i = 0; i < arity() - 1; i++) {
			disallowAt(i, alt);
			for (Set<Alt> as: alt.getDisallowed()) {
				for (Alt a: as) {
					if (a.getNonTerminal().equals(alt.getNonTerminal())) {
						disallowAt(i, a);
					}
				}
			}
		}
	}
	
	
	public boolean isAllowedAt(int i, Alt alt) {
		if (disallowed[i] != null) {
			return !disallowed[i].contains(alt);
		}
		return true;
	}
	
	@Override
	public void parse(int pos, GSS cu, Node cn, GLL ctx) {
		if (elts.isEmpty()) {
			Node cr = ctx.getNodeE(pos); 
			cn = ctx.getNodeP(items[0], cn, cr);
			ctx.pop(cu, cn, pos);
			return;
		}
		else {
			parseAt(0, pos, cu, cn, ctx);
		}
	}
	
	void parseAt(int dot, int pos, GSS cu, Node cn, GLL ctx) {
		for (int i = dot; i < elts.size(); i++) {
			
			
			Symbol sym = elts.get(i);
			
			
			if (sym instanceof Terminal) {
				
				if (ctx.isAtEnd(pos)) {
					return;
				}
				
				Terminal trm = (Terminal)sym;

				String value = trm.match(pos, ctx);
				if (value != null) {
					int skip = value.length();
					Node cr = ctx.getNodeT(trm, value, pos, skip);
					cn = ctx.getNodeP(items[i+1], cn, cr);
					pos += skip;
					
					// This implements the fall through in the GLL paper
					continue;
				}
				return;
			}
			
			// && test(ctx.charAt(pos), (NonTerminal)sym, i+1)
			if (sym instanceof NonTerminal) {
				cu = ctx.create(items[i+1], cu, cn, pos);
				for (Alt alt: (NonTerminal)sym) {
					if (isAllowedAt(i, alt)) {
						ctx.add(alt, cu, pos, null);
					}
				}
				return;
			}
			
			throw new AssertionError("invalid parser: " + sym);
		}
		ctx.pop(cu, cn, pos);
	}

	
	private boolean test(char c, NonTerminal sym, int i) {
		return true;
//		if (inFirst(i, c)) {
//			return true;
//		}
//		if (isNullable(i) && sym.isInFollow(c)) {
//			
//		}
//		return false;
	}
	
	
	

//	private boolean inFirst(int i, char c) {
//		for (int j = i; j < arity(); j++) {
//			if (get(j).isInFirst(c)) {
//				return true;
//			}
//			if (get(j).isNullable()) {
//				continue;
//			}
//			return false;
//		}
//		return false;
//	}

	@Override
	public String toString() {
		return nonTerminal + "::=" + elts.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return false;
	}

	public void setNonTerminal(NonTerminal nonTerminal) {
		this.nonTerminal = nonTerminal;
	}


	public NonTerminal getNonTerminal() {
		return nonTerminal;
	}
	
	public boolean isNullable() {
		return isNullable(0);
	}

	private boolean isNullable(int i) {
		for (int j = i; j < arity(); j++) {
			if (!get(j).isNullable()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isInFirst(char c) {
		assert !elts.isEmpty();
		return false;
	}
	
}
