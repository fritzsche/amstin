package amstin.models.entity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import amstin.Config;
import amstin.models.grammar.Grammar;
import amstin.models.meta.MetaModel;
import amstin.parsing.Parser;
import amstin.tools.InferMetaModel;
import amstin.tools.MetaModelToJava;
import amstin.tools.Unparse;

import com.sun.codemodel.JClassAlreadyExistsException;

public class _Main {
	
	private static final String ENTITY_DIR = Config.PKG_DIR + "/models/entity";
	private static final String ENTITY_PKG = Config.PKG +".models.entity";
	private static final String ENTITY_MDG = ENTITY_DIR + "/entity.mdg";
	private static final String ENTITY_META = ENTITY_DIR + "/entity.meta";

	public static void main(String[] args) throws IOException, JClassAlreadyExistsException {
		Grammar entityGrammar = Parser.parseGrammar(ENTITY_MDG);
		System.out.println(entityGrammar);
		MetaModel entityMetaModel = InferMetaModel.infer("Entity", entityGrammar);
		PrintWriter writer = new PrintWriter(System.out);
		
		Grammar metaGrammar = Parser.parseGrammar(amstin.models.meta._Main.METAMODEL_MDG);
		Unparse.unparse(metaGrammar, entityMetaModel, writer);
		writer.flush();
		
		

		File file = new File(Config.ROOT);
		
		MetaModelToJava.metaModelToJava(file, ENTITY_PKG, entityMetaModel);
		
	}
	
	
}