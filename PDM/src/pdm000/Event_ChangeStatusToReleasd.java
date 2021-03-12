package pdm000;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.andy.plm.dblog.DBLogIt;

import connector.agile.AgileSession_Connection;

/**
 * 用於CheckPoint2站別，自動切到Releasd站別，
 * 
 * @author andy_chuang
 * @version %i%, %g%
 */
public class Event_ChangeStatusToReleasd implements IEventAction {

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	DBLogIt log = new DBLogIt(); // 寫Log到資料庫
	IAgileSession session = null;

	
	// /**********************************************************************
	// * 2.其他全域變數宣告
	// **********************************************************************/
	//
	// /**********************************************************************
	// * 3.主程式(請勿修改位置)
	// **********************************************************************/
	// @SuppressWarnings("finally")
	// public ActionResult doAction(IAgileSession session, INode node,
	// IDataObject obj) {
	// /**********************************************************************
	// * 3-1.dblog參數設定
	// ********************************************************************/
	// log.setUserInformation(session);
	// log.setPxInfo(obj);
	// log.setPgName("PX_changeStatusToReleasd");// 自行定義程式名稱
	// // log.dblog("程式開始時間:" + getCurrentDateTime());
	// /**********************************************************************
	// * 3-2.doAction
	// **********************************************************************/
	// this.session = session;
	// try {
	// IChange change = (IChange) obj;
	// change.getSession().disableAllWarnings();
	// change.refresh();
	// change.changeStatus(change.getDefaultNextStatus(), false, "", false,
	// false, null, null, null, null, false);
	// change.getSession().enableAllWarnings();
	// session.enableAllWarnings();
	// } catch (Exception e) {
	// log.setErrorMsg(e);
	// } finally {
	// log.updataDBLog();
	// return new ActionResult(ActionResult.STRING, log.getResult());
	// }
	// }
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setEventInfo(info);
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		IWFChangeStatusEventInfo Info = (IWFChangeStatusEventInfo) info;
		try {
			IChange change = (IChange) Info.getDataObject();
			change.getSession().disableAllWarnings();
			change.refresh();
			change.changeStatus(change.getDefaultNextStatus(), false, "", false, false, null, null, null, null, false);
			change.getSession().enableAllWarnings();
			session.enableAllWarnings();
		} catch (APIException e) {
			e.printStackTrace();
		}
		return new EventActionResult(Info, new ActionResult(ActionResult.STRING, "OK"));
	}

}