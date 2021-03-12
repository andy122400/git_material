package pdm000;

import com.agile.api.IAgileSession;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.sun.corba.se.spi.orbutil.fsm.Action;
import com.sun.org.apache.bcel.internal.generic.AALOAD;

public class Test_Function implements ICustomAction{

	@Override
	public ActionResult doAction(IAgileSession arg0, INode arg1, IDataObject arg2) {
		// TODO Auto-generated method stub
		Event_AutoChangeNumber pg1 = new Event_AutoChangeNumber();
		Px_AutoApprover_check pg2 = new Px_AutoApprover_check();
		//SYSO
		return new ActionResult(ActionResult.STRING,pg2.testFunction());
	}
	

}
