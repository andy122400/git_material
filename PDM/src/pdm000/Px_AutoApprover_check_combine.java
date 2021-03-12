package pdm000;

import java.sql.Connection;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.dblog.DBLogIt;
import com.andy.plm.html.HtmlFormat;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年10月7日 Description: program logic: 1.
 **********************************************************************/

public class Px_AutoApprover_check_combine implements ICustomAction {

	// 測試Function
	// public static void main(String[] args) throws APIException {
	// Px_AutoApprover_check_combine temp = new Px_AutoApprover_check_combine();
	// IAgileSession session =
	// connector.agile.AgileSession_Connection.getAgileAdminSession();
	// IDataObject obj = (IDataObject) session.getObject(IChange.OBJECT_TYPE,
	// "POA201000001");
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
	@SuppressWarnings("finally")
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("pdm000.Px_AutoApprover_check_combine2");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		IChange currentchange = (IChange) obj;
		Px_AutoApprover_check AutoApprover = new Px_AutoApprover_check();
		Px_AutoApprover_check_ECN AutoApprover_ECN = new Px_AutoApprover_check_ECN();
		try {

			AutoApprover.doAction(session, node, obj);
			AutoApprover_ECN.doAction(session, node, obj);
			sql_Error_Message = AutoApprover.getCheckResult();
			sql_Error_Message1 = AutoApprover_ECN.getCheckResult();
			if (!sql_Error_Message.equals(sql_Error_Message1)) {
				sql_Error_Message = sql_Error_Message + "\n" + sql_Error_Message1;
				sql_Error_Message = sql_Error_Message.replace("檢查完畢，沒有錯誤", "");
			}
			Result_verification = AutoApprover.isCheckError();
			if (Result_verification)
				Result_verification = AutoApprover_ECN.isCheckError();

			html_header = HtmlFormat.Html_Header("簽核前，Team Member 人員檢查程式", "WinSome(1573)", sql_Error_Message+"\n"+""+"如發現錯誤，請通知該單位主管 到(PIS011)專案人員清單 Project & Model Member，維護上述相關簽核人員\n\n"
			+"<span style=\"font-size:24px; color: red \"><b>請別找MIS，謝謝。</b></span>",
					obj);

		} catch (Exception e) {
			log.setErrorMsg(e);

		} finally {
			if (!Result_verification)
				return new ActionResult(ActionResult.EXCEPTION, new Exception(AutoApprover.getError()));
			else {
				if (sql_Error_Message.equals("檢查完畢，沒有錯誤")) {
				
					try {
						currentchange.getCell(1271).setValue("Yes");
					} catch (APIException e) {
						e.printStackTrace();
					}
				}
				return new ActionResult(ActionResult.STRING,html_header);
			}
		}

	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	// private void init() {
	// }

	/**********************************************************************
	 * 5.解構&釋放
	 **********************************************************************/
	// private void close() {
	// try {
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/

}
