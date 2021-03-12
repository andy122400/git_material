package pdm000;

import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IRowReferenceObjectWebServiceSearch;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;

import javax.sound.sampled.LineListener;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IObjectEventInfo;
import com.agile.px.ISignOffEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.andy.plm.dblog.DBLogIt;
import com.sun.activation.registries.MailcapParseException;
import com.sun.media.jfxmedia.control.VideoDataBuffer;
import com.sun.org.apache.regexp.internal.recompile;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2021 2021年2月18日 Description: program logic: 1.
 **********************************************************************/

public class PxEvent_NotificationExtension implements ICustomAction, IEventAction {

	// 測試Function
	public static void main(String[] args) throws APIException {
		PxEvent_NotificationExtension temp = new PxEvent_NotificationExtension();
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		IDataObject obj = (IDataObject) session.getObject(IChange.OBJECT_TYPE, "POA201200021");
		temp.doAction(session, null, obj);
	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	// Connection msdb =
	// connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	Set<IUser> set_Notified_Object = new HashSet<>();
	String notify_type, condition_verification, item_source, team_member_type, notify_object_by_team_member,
			notify_object_by_cell, notify_object_by_custom, notify_template_function, custom_content_key, custom_text;
	private IChange change;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("PxEvent_NotificationExtension");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {
			// 1. 取得Agile common欄位資料(站別、流程)
			IChange change = (IChange) obj;
			IWorkflow workflow = change.getWorkflow();
			IStatus status = change.getStatus();

			// 2. 取得資料庫table欄位資料 (Select * from table where enable=y ,站別,流程)
			Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-1-2");// 測試機
			Statement stmt = msdb.createStatement();
			String sql = "select * from pdmdb.dbo.real_agile_mail_notification with(nolock)  " + "where [workflow]='"
					+ workflow + "' " + "and [enable]='Y' " + "and [status] ='" + status + "'";
			System.out.println(sql);
			ResultSet rs = stmt.executeQuery(sql);
			List<Map<String, Object>> list = selectAll(rs);
			msdb.close();
			String body;
			for (Map<String, Object> map : list) {
				getSqlRowData(map);

				if (!verification(change)) { // 判斷是否符合條件
					continue;
				}
				switch (notify_type) {
				case "1":
					get_Notified_Object_By_Customize(change, notify_object_by_custom);
					get_Notified_Object_By_ChangeCell(change, notify_object_by_cell);
					get_Notified_Object_By_TeamMember(change, notify_object_by_team_member);
					break;
				}

				// GetBody

			}

			// 3. 如果cell_1 空，則就通知 且如果cell_2 空，則就通知，其餘比對
			// System.out.println(condition_verification(change, "2000003298",
			// ""));

			// 3.1如果cell_1 或 如果cell_2 比對失敗就退出
			// 4. 一個UserSet容器

			// 6. notify_object_by_cell，根據欄位取得每個使用者，並加入User容器(如果空就pass)
//			System.out.println(notify_template_function);
//			System.out.println(set_Notified_Object);
//			System.out.println(custom_text);
			// 再根據模板 寄件出去
			session.sendNotification(change, notify_template_function, set_Notified_Object, false, custom_text);
			log.dblog("notify_template_function:" + notify_template_function.toString());
			log.dblog("Notifyer:" + set_Notified_Object.toString());
			System.out.println("Notifyer:" + set_Notified_Object.toString());
		} catch (Exception e) {
			e.printStackTrace();
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			return new ActionResult(ActionResult.STRING, log.getResult());
		}

	}

	private boolean verification(IChange change) throws APIException {
		String[] condition_verification_Array = condition_verification.split(";");
		boolean result = true;
		for (String condition : condition_verification_Array) {
			System.out.println("AA:" + condition);
			if (condition.contains("!=")) {
				if (condition_verification(change, condition.split("!=")[0], condition.split("!=")[1])) {
					result = false;
					break;
				}
			} else if (condition.contains("=")) {
				if (!condition_verification(change, condition.split("=")[0], condition.split("=")[1])) {
					result = false;
					break;
				}
			}
		}
		return result;
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
	 * 
	 * @throws APIException
	 * @throws NumberFormatException
	 **********************************************************************/

	void get_Notified_Object_By_ChangeCell(IChange change, String cellId) throws NumberFormatException, APIException {
		System.out.println("進入 get_Notified_Object_By_ChangeCell");
		if (!cellId.equals("")) {// 如果沒填寫cellId欄位，則不需要通知
			IAgileList AgileList = (IAgileList) change.getCell(Integer.valueOf(cellId)).getValue();
			IAgileList[] AgileList_array = AgileList.getSelection();
			for (IAgileList list : AgileList_array) {
				IUser user = (IUser) list.getValue();
				System.out.println("AA"+user);
				set_Notified_Object.add(user);
			}
		} else {
			System.out.println("沒有維護cellId欄位");
		}
	}

	void get_Notified_Object_By_Customize(IChange change, String customizeUser)
			throws NumberFormatException, APIException {
		// 如果excel維護錯誤 或者Agile系統沒有對象 要卡控

		System.out.println("進入 get_Notified_Object_By_Customize");
		if (!customizeUser.equals("")) {
			String[] customizeUsers = customizeUser.split(";");
			for (String User : customizeUsers) {
				IUser user = (IUser) this.session.getObject(IUser.OBJECT_TYPE, User);
				set_Notified_Object.add(user);
			}
		} else {
			System.out.println("沒有維護customizeUser欄位");
		}
	}

	void get_Notified_Object_By_TeamMember(IChange change, String Division)
			throws NumberFormatException, APIException, SQLException {
		System.out.println("進入 get_Notified_Object_By_TeamMember");
		if (!Division.equals("")) {// 如果沒填寫cellId欄位，則不需要通知
			Division = Division.replace(';', ',');
			// String FatherNumber =
			// get_FatherNumber(get_ChagneAffectedItemNumber(change)); //先不用
			Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
			Statement stmt = msdb.createStatement();
			String Sql = null;

			if (!item_source.equals("af_Item")) {
				String objectNumber = change.getValue(Integer.valueOf(item_source)).toString().replace(";", ",");
				if (team_member_type.equals("Modle")) {

					Sql = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + objectNumber + "','" + Division + "'";
				} else if (team_member_type.equals("Project")) {

					Sql = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + objectNumber + "','" + Division + "'";
				}
			} else {
				System.out.println(get_FatherNumber(get_ChagneAffectedItemNumber(change)));
				String objectNumber = get_FatherNumber(get_ChagneAffectedItemNumber(change));
				Sql = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + objectNumber + "','" + Division + "'";
			}
			System.out.println(Sql);
			ResultSet rs = stmt.executeQuery(Sql);
			List<Map<String, Object>> list = selectAll(rs);

			for (Map<String, Object> map : list) {
				IUser user = (IUser) this.session.getObject(IUser.OBJECT_TYPE, map.get("user_logon"));
				System.out.println(user);
				if (user != null) {
					set_Notified_Object.add(user);
				}
			}
			// System.out.println(list);
			msdb.close();
		} else {
			System.out.println("沒有維護Division欄位");
		}
		// String[] Divisions = Division.split(";");
		// for (String User : Divisions) {
		// IUser user = (IUser) this.session.getObject(IUser.OBJECT_TYPE, User);
		// set_Notified_Object.add(user);
		// }
	}

	String get_FatherNumber(String childNumber) throws SQLException {
		System.out.println("\t進行get_FatherNumber");
		Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
		Statement stmt = msdb.createStatement();
		String Sql = "exec pdmdb.dbo.zp_agile_get_fpartno  '" + childNumber + "'";
		ResultSet rs = stmt.executeQuery(Sql);
		StringBuffer sb = new StringBuffer("");
		while (rs.next()) {
			sb.append(rs.getString(1).trim()).append(",");
		}
		msdb.close();
		return sb.toString();
	}

	String get_ChagneAffectedItemNumber(IChange change) throws APIException {
		System.out.println("\t進行get_ChagneAffectedItemNumber");
		ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = table.iterator();
		StringBuffer sb = new StringBuffer("");
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			IItem item = (IItem) row.getReferent();
			sb.append(item).append(",");
		}
		// System.out.println(sb);
		return sb.toString();
	}

	private boolean condition_verification(IChange change, String cellID, String string) throws APIException {
		System.out.println("進行condition_verification比對");
		if (!cellID.equals("") && !string.equals("")) {
			String value = change.getValue(Integer.valueOf(cellID)).toString();
			if (value.equals(string))
				return true;
			else {
				return false;
			}
		} else {
			System.out.println("沒有維護資料");
			return false;
		}
	}

	private void getSqlRowData(Map<String, Object> map) {
		notify_type = (String) map.get("notify_type");
		condition_verification = (String) map.get("condition_verification");
		item_source = (String) map.get("item_source");
		team_member_type = (String) map.get("team_member_type");
		notify_object_by_team_member = (String) map.get("notify_object_by_team_member");
		notify_object_by_cell = (String) map.get("notify_object_by_cell");
		notify_object_by_custom = (String) map.get("notify_object_by_custom");
		notify_template_function = (String) map.get("notify_template_function");
		custom_content_key = (String) map.get("custom_content_key");
		custom_text = (String) map.get("custom_text");
		System.out.println(notify_type);
		System.out.println(condition_verification);
		System.out.println(item_source);
		System.out.println(team_member_type);
		System.out.println(notify_object_by_team_member);
		System.out.println(notify_object_by_cell);
		System.out.println(notify_object_by_custom);
		System.out.println(notify_template_function);
		System.out.println(custom_content_key);

	}

	public List<Map<String, Object>> selectAll(ResultSet rs) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			// 獲取結果集結構（元素據）
			ResultSetMetaData rmd = rs.getMetaData();
			// 獲取欄位數（即每條記錄有多少個欄位）
			int columnCount = rmd.getColumnCount();
			while (rs.next()) {
				// 儲存記錄中的每個<欄位名-欄位值>
				Map<String, Object> rowData = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; ++i) {
					// <欄位名-欄位值>
					rowData.put(rmd.getColumnName(i), rs.getObject(i));
				}
				// 獲取到了一條記錄，放入list
				list.add(rowData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return list;
	}

	@Override
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		log.setUserInformation(session);
		log.setEventInfo(info);
		this.session = session;
		PxEvent_NotificationExtension temp = new PxEvent_NotificationExtension();
		IWFChangeStatusEventInfo Info = (IWFChangeStatusEventInfo) info;
		try {
			change = (IChange) Info.getDataObject();

		} catch (APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new EventActionResult(info, temp.doAction(session, node, change));
	}
}
