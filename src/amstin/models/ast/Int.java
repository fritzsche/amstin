
package amstin.models.ast;


public class Int
    extends Tree
{

    public Integer value;
    public Location loc;

    @Override
    public String toString() {
    	return value.toString();
    }
}
