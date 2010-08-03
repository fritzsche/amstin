package amstin.models.grammar.parsing;

import java.util.Map;

public interface IParser {
	public void parse(Map<IParser, Map<Integer, Entry>> table, Cnt cnt, String src, int pos);
}
