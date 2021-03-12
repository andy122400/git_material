package pdm000;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.accton.acctonRule;
import com.andy.plm.html.HtmlFormat;
import com.sun.org.apache.bcel.internal.generic.AALOAD;

import sun.util.logging.resources.logging_fr;

import java.sql.Connection;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IObjectEventInfo;
import com.andy.plm.dblog.DBLogIt;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年10月13日 Description: program logic: 1.
 **********************************************************************/

public class Px_AutoApprover_set_combine implements ICustomAction {

	// 測試Function
	// public static void main(String[] args) throws APIException {
	// Px_AutoSetApprover_combine temp = new Px_AutoSetApprover_combine();
	// IAgileSession session =
	// connector.agile.AgileSession_Connection.getAgileAdminSession();
	// IDataObject obj =(IDataObject)session.getObject(IChange.OBJECT_TYPE,
	// arg1);
	// temp.doAction(session, null, obj);
	// }

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	String html_header;
	Boolean Result_verification = true;
	String sql_Error_Message = "";
	String sql_Error_Message1 = "";
	String error = "";

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("pdm000.Px_AutoApprover_set_combine");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {

			IChange currentchange = (IChange) obj;
			Px_AutoApprover_set aa = new Px_AutoApprover_set();
			Px_AutoApprover_set_ECN bb = new Px_AutoApprover_set_ECN();
			aa.doAction(session, node, obj);
			bb.doAction(session, node, obj);

		} catch (Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			return new ActionResult(ActionResult.STRING, log.getResult());
		}

	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	private void init() {
	}

	/**********************************************************************
	 * 5.解構&釋放
	 **********************************************************************/
	private void close() {
		try {
		} catch (Exception e) {
		}
	}

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/

}
