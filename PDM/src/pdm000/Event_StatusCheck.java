package pdm000;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLSession;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.EventConstants;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.ISignOffEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.andy.plm.dblog.DBLogIt;

import connector.db.SqlServer_Connection;
import javafx.collections.ListChangeListener.Change;

/**************************************************************
 * Editor: Last Modify: 2020年10月26日 Description:當Create時，自動將表單單號生成 program
 * logic: 1. 2. 3. 4.
 **********************************************************************/
public class Event_StatusCheck implements IEventAction {
	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	ArrayList<IItem> overSizeList = new ArrayList<IItem>();
	Map<IItem, Integer> map = new HashMap<>();
	boolean verification = true;
	IChange change;
	Integer item_title_number = 1001;
	Integer changerequest_coverpage_number = 1047;
	Integer change_cp_changetype = 1069;
	Integer pcm011_p3_CustomerModelNo = 1599;
	String change_number = "";
	IAgileSession session;
	String result = "";
	Integer part_p3_buy_type = 1548;// 1548 買賣料類別

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
			case EventConstants.EVENT_APPROVE_FOR_WORKFLOW:
				ISignOffEventInfo SignOffEventInfo = (ISignOffEventInfo) info;
				if (SignOffEventInfo.getDataObject().getType() == IChange.OBJECT_TYPE) {
					change = (IChange) SignOffEventInfo.getDataObject();
					if ((change.getWorkflow().getName().equals("Part PhaseOut Apply Workflow")
							&& change.getStatus().getName().equals("40.CE"))
							|| !change.getWorkflow().getName().equals("Part PhaseOut Apply Workflow")) {
						check_DescriptionOverSize(); // Phantom Part Request
														// Workflow 只有CE站別要判斷
														// 20201117討論結果
					}
					// if (change.getWorkflow().getName().equals("Description
					// Update Workflow")
					// && change.getStatus().getName().equals("40.CE Review")
					// || change.getWorkflow().getName().equals("Initial Part
					// Apply Workflow")
					// && change.getStatus().getName().equals("80.CE Review")
					// || change.getWorkflow().getName().equals("Conditional
					// Approval Workflow")
					// && change.getStatus().getName().equals("50.IQC")
					// || change.getWorkflow().getName().equals("Part PhaseOut
					// Apply Workflow")
					// && change.getStatus().getName().equals("40.CE")
					// || change.getWorkflow().getName().equals("Phantom Part
					// Request Workflow")
					// && change.getStatus().getName().equals("50.CE Review")) {
					// check_DescriptionOverSize();
					check_CustomerModel();
					// }
					break;
				}
			case EventConstants.EVENT_CHANGE_STATUS_FOR_WORKFLOW:
				IWFChangeStatusEventInfo ChangeStatusEventInfo = (IWFChangeStatusEventInfo) info;
				if (ChangeStatusEventInfo.getDataObject().getType() == IChange.OBJECT_TYPE) {
					change = (IChange) ChangeStatusEventInfo.getDataObject();
					check_DescriptionOverSize();
					check_CustomerModel();
					check_RecursiveBOM();
					break;
				}
			}

			// ISignOffEventInfo Info = (ISignOffEventInfo) info;

		} catch (

		Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();

			// return new EventActionResult(info, new
			// ActionResult(ActionResult.STRING, log.getResult()));
			if (verification)
				return new EventActionResult(info, new ActionResult(ActionResult.STRING, log.getResult()));
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
		} catch (Exception e) {
		}
	}

	/**********************************************************************
	 * 6.以下為Biz Function
	 * 
	 * @throws APIException
	 **********************************************************************/

	public void check_DescriptionOverSize() throws APIException {
		ITable affectedTable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedTable.iterator();
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			IItem item = (IItem) row.getReferent();
			if (item.getAgileClass().getSuperClass().getName().equals("Parts")) {
				String Description = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION).getValue()
						.toString();
				if (Description.length() > 60) {
					verification = false;
					// overSizeList.add(item);
					map.put(item, Description.length());
					result += "描述超過六十碼:" + map.toString() + "\n";
					log.dblog(result);
				}
			}
			// log.dblog("料號:" + item + "\t" + "描述長度:" +
			// Description.length());
		}
	}

	public void check_CustomerModel() throws APIException {

		// Integer change_cp_changetype = 1069;
		// Integer part_p3_buy_type = 1548;//1548 買賣料類別
		ICell changetype = change.getCell(change_cp_changetype);
		switch (changetype.getValue().toString().substring(1, 7).toString()) {
		case "PCM011":
			IRow afrow;
			ITable aftable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator afit = aftable.iterator();
			while (afit.hasNext()) {
				afrow = (IRow) afit.next(); // 下一筆row
				String item_number = afrow.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).toString();
				String item_type = afrow.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();

				switch (item_type) {
				case "F PROD": // F C P
					ICell buy_type = Get_RedlineCell(part_p3_buy_type, afrow, item_number);
					if (buy_type.getValue().toString().equals("")) {
						ICell ModelnoCell = Get_RedlineCell(pcm011_p3_CustomerModelNo, afrow, item_number);
						System.out.println(ModelnoCell.getValue().toString());
						String result_string = Check_model_stage(ModelnoCell.getValue().toString());
						if (result_string.equals("0")) {
							System.out.println(item_number + " [Customer Model No (客戶型號) 重複 !!!] ");
							result += item_number + " [Customer Model No (客戶型號) 重複 !!!] " + "\n";
							verification = false;
						}
					}
					break;
				default:
					break;
				}
			}
			break;
		default:
			break;
		}

	}

	public String Check_model_stage(String model_no) {
		String returnString = "";

		try {
			Connection msdb = SqlServer_Connection.getMsDbConn("ERP-0-2"); // ms資料庫連線
			Statement stmt = msdb.createStatement();

			String sql_string = "EXEC zp_notes_model_stage_check '','" + model_no + "',4,'05'  ";
			// String sql_string = "zp_notes_model_stage_check '','OAP100
			// (C)',4,'05' ";

			ResultSet rs = stmt.executeQuery(sql_string);
			while (rs.next()) {
				returnString = rs.getString("flag").toString();
			}
			msdb.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return returnString;
	}

	public ICell Get_RedlineCell(Integer baseID, IRow cur_afrow, String numberID) {
		ICell return_cell = null;

		try {
			IItem redIItem = (IItem) cur_afrow.getReferent();

			ITable RedlineTable_P3 = redIItem.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
			ITable RedlineTable_P2 = redIItem.getTable(ItemConstants.TABLE_REDLINEPAGETWO);
			// ITable RedlineTable_TB =
			// redIItem.getTable(ItemConstants.TABLE_REDLINETITLEBLOCK);

			IRow redlineP3Row = (IRow) RedlineTable_P3.iterator().next();
			ICell Redcell3 = redlineP3Row.getCell(baseID);
			if (Redcell3 == null) {
				// System.out.println("No P3");
			} else {
				System.out.println("P3=" + Redcell3.getValue().toString());
				return_cell = Redcell3;
			}

			IRow redlineP2Row = (IRow) RedlineTable_P2.iterator().next();
			ICell Redcell2 = redlineP2Row.getCell(baseID);
			if (Redcell2 == null) {
				// System.out.println("No P2");
			} else {
				System.out.println("P2=" + Redcell2.getValue().toString());
				return_cell = Redcell2;
			}

			if (return_cell == null) {
				IItem iitem = (IItem) session.getObject(IItem.OBJECT_TYPE, numberID);
				ICell itemcell = iitem.getCell(baseID);
				if (itemcell == null) {
					// System.out.println("No itemcell");
				} else {
					System.out.println("itemcell=" + itemcell.getValue().toString());
					return_cell = itemcell;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return return_cell;
	}

	void check_RecursiveBOM() throws APIException {
		if (change.audit(true).toString().contains("has recursive BOM")) {
			result += "違反BOM規則，父階是子階的爸爸，又是子階的兒子";
			verification = false;
		}
	}
}
