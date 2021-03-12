package pdm000;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ItemConstants;
import com.agile.api.StatusConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.EventConstants;
import com.agile.px.IEventAction;
import com.agile.px.IEventDirtyRow;
import com.agile.px.IEventDirtyRowUpdate;
import com.agile.px.IEventDirtyTable;
import com.agile.px.IEventInfo;
import com.agile.px.IUpdateTableEventInfo;
import com.andy.plm.dblog.DBLogIt;

/**************************************************************
 * Editor: Last Modify: 2021年1月12日 Description:當Create時，自動將表單單號生成 program logic:
 * 1. 2. 3. 4.
 **********************************************************************/
public class Event_Control_Updatetable implements IEventAction {
	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	String result = "";
	private String changeTypeName;
	boolean tag = true;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setEventInfo(info);
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		try {
			IUpdateTableEventInfo Info = (IUpdateTableEventInfo) info;
			IChange change = (IChange) Info.getDataObject();
			// changeTypeName
			// =change.getValue(ChangeConstants.ATT_COVER_PAGE_CHANGE_TYPE).toString();
			changeTypeName = change.getAgileClass().getName().toString();
			if (change.getValue(ChangeConstants.ATT_COVER_PAGE_STATUS).toString().equals("10.Applicant")
					|| change.getValue(ChangeConstants.ATT_COVER_PAGE_STATUS).toString().equals("Unassigned")) {
				IEventDirtyTable Table = Info.getTable();
				Iterator it2 = Table.iterator();
				while (it2.hasNext()) {
					IEventDirtyRowUpdate row = (IEventDirtyRowUpdate) it2.next();
					// IEventDirtyRow row = (IEventDirtyRow) it2.next();
					if (row.getAction() == EventConstants.DIRTY_ROW_ACTION_ADD) {
						IItem item = (IItem) row.getReferent();
						if (hasSamePendingChangeType(item))
							result = "[" + item + "]" + "該料件，在其他[" + changeTypeName + "]表單中也有使用到";
						else if (changeTypeName.equals("(PCM019)Priority Change Apply")) { // 針對ECN&PCA互卡
							if (hasPendingPCM007(item))
								result = "[" + item + "]" + "該料件，在其他[" + "(PCM007)Engineering Change Notices"
										+ "]表單中也有使用到";
						} else if (changeTypeName.equals("(PCM007)Engineering Change Notices")) {// 針對ECN&PCA互卡
							if (hasPendingPCM019(item))
								result = "[" + item + "]" + "該料件，在其他[" + "(PCM019)Priority Change Apply" + "]表單中也有使用到";
						}
					}
				}
			}
		} catch (

		Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			close();
			if (tag)
				return new EventActionResult(info, new ActionResult(ActionResult.STRING, result));
			else
				return new EventActionResult(info, new ActionResult(ActionResult.EXCEPTION, new Exception(result)));
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
			// adminSession.close();
		} catch (Exception e) {
		}
	}

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/

	public boolean hasSamePendingChangeType(IItem item) throws APIException {
		ITable table = item.getTable(ItemConstants.TABLE_PENDINGCHANGES);
		Iterator<?> it = table.iterator();
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			if (row.getValue(1070).toString().equals(changeTypeName)) {
				tag = false;
				return true;
			}
		}
		return false;
	}

	public boolean hasPendingPCM019(IItem item) throws APIException {
		ITable table = item.getTable(ItemConstants.TABLE_PENDINGCHANGES);
		Iterator<?> it = table.iterator();
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			if (row.getValue(1070).toString().equals("(PCM019)Priority Change Apply")) {
				tag = false;
				return true;
			}
		}
		return false;
	}

	public boolean hasPendingPCM007(IItem item) throws APIException {
		ITable table = item.getTable(ItemConstants.TABLE_PENDINGCHANGES);
		Iterator<?> it = table.iterator();
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			if (row.getValue(1070).toString().equals("(PCM007)Engineering Change Notices")) {
				tag = false;
				return true;
			}
		}
		return false;
	}
}
