package pdm000;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.INode;
import com.agile.api.IQuery;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.EventConstants;
import com.agile.px.ICreateEventInfo;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.ISaveAsEventInfo;
import com.andy.plm.accton.util;
import com.andy.plm.dblog.DBLogIt;

/**************************************************************
 * Editor:Andy_chuang Last Modify:20202020年7月9日 Description:當Create時，自動將表單單號生成
 * program logic: 1. 2. 3. 4. Remark: 針對Create、S
 **********************************************************************/
public class Event_AutoChangeNumber implements IEventAction {

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	// IAgileSession adminSession = ConnectAll.getAgileAdminSession();//
	// 取得管理者最高權限
	// Connection msdb=ConnectAll.getMsDbConn(); //ms資料庫連線
	// Connection oracledb=ConnectAll.getOracleDbConn(); //oracle資料庫連線
	IAgileSession session;
	DBLogIt log = new DBLogIt();
	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	IChange change;
	String seqNumber;
	String changeMaxNumber;
	String old_change;
	String new_change;
	Integer change_pagetwo_SDKApproverCheck = 1271;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	@SuppressWarnings("finally")
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setEventInfo(info);
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {

			switch (info.getEventType()) {

			case EventConstants.EVENT_CREATE_OBJECT:
				ICreateEventInfo createInfo = (ICreateEventInfo) info;
				change = (IChange) createInfo.getDataObject();
				break;
			case EventConstants.EVENT_SAVE_AS_OBJECT:
				ISaveAsEventInfo saveAsInfo = (ISaveAsEventInfo) info;
				change = (IChange) session.getObject(ChangeConstants.CLASS_CHANGE_BASE_CLASS,
						saveAsInfo.getNewNumber());
				break;
			}
			old_change = change.getName();
			setNewChangeNumber();
			log.dblog("舊單號[" + old_change + "],更改為新單號[" + new_change + "]");

		} catch (Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			return new EventActionResult(info, new ActionResult(ActionResult.STRING, log.getResult()));
		}

	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	// private void init() {
	//
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
	/**********************************************************************
	 * 取得目前時間(年月MMdd)
	 **********************************************************************/
	private static String getCurrentDateTime() {
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyyMM");
		sdFormat.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		Date date = new Date();
		String strDate = sdFormat.format(date).substring(2);
		return strDate;
	}

	/**********************************************************************
	 * 取得表單類型資訊(前三碼)
	 **********************************************************************/
	String getChangeType() throws APIException {
		String temp = change.getCell(ChangeConstants.ATT_COVER_PAGE_NUMBER).getValue().toString().substring(0, 3);
		return temp;
	}

	/**********************************************************************
	 * 寫入最新表單單號
	 **********************************************************************/
	void setNewChangeNumber() throws APIException {
		new_change = getChangeType() + getCurrentDateTime() + getMaxSequence();
		change.setValue(ChangeConstants.ATT_COVER_PAGE_NUMBER, new_change);
	}

	/**********************************************************************
	 * 取得最新表單單號
	 **********************************************************************/
	public String getNewChangeNumber() throws APIException {
		return getChangeType() + getCurrentDateTime() + getMaxSequence();
	}

	/**********************************************************************
	 * 判斷Table是否為空
	 **********************************************************************/
	boolean isTableNull(ITable table) {
		Boolean result = true;

		if (table.size() != 0)
			result = false;
		return result;
	}

	/**********************************************************************
	 * 取得表單流水碼
	 **********************************************************************/
	String getMaxSequence() throws APIException {
		IQuery query = (IQuery) session.createObject(IQuery.OBJECT_TYPE, ChangeConstants.CLASS_CHANGE_BASE_CLASS);
		String Criteria = " SELECT " + "[Cover Page.Number] " + " FROM " + "[Changes]" + " WHERE "
				+ "[Cover Page.Number]" + " LIKE " + "'" + getChangeType() + getCurrentDateTime() + "%'" + " ORDER BY "
				+ "1 desc";
		query.setCriteria(Criteria);
		ITable table = query.execute();
		Iterator<?> it = table.iterator();
		if (!isTableNull(table)) {
			IRow row = (IRow) it.next();
			changeMaxNumber = (String) row.getCell(ChangeConstants.ATT_COVER_PAGE_NUMBER).getValue();
			seqNumber = String.valueOf((Integer.parseInt(changeMaxNumber.substring(9)) + 10000001));
			seqNumber = seqNumber.substring(3);
		} else
			seqNumber = "00001";
		log.dblog("取得流水號[" + seqNumber + "]");
		return seqNumber;
	}
}