
package amstin.models.ast;


public class Def
    extends Tree
{

    public String name;
    public Location loc;

    @Override
    public String toString() {
    	return "&" + name;
    }
}