package pdm000;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IAttribute;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRedlinedTable;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.ICustomAction;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.andy.plm.dblog.DBLogIt;
import com.andy.plm.html.HtmlFormat;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年11月17日 Description: program logic: 1.
 **********************************************************************/

public class Event_BomCheck implements ICustomAction, IEventAction {

	// 測試Function
	public static void main(String[] args) throws APIException {
		Event_BomCheck temp = new Event_BomCheck();
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		IChange obj = (IChange) session.getObject(IChange.OBJECT_TYPE, "ECN201200013");
		IItem obj2 = (IItem) session.getObject(IItem.OBJECT_TYPE, "TEMP000000825");
//		IChange obj2 =(IChange) obj;
		System.out.println(obj2.getAgileClass().getSuperClass().getName());
		// temp.doAction(session, null, obj);
	}
	//

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	MultiMap<String, String> mapp = new MultiValueMap<>();
	String result = "";
	String html_header;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	@SuppressWarnings({ "finally", "finally", "finally" })
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("pdm000.Event_BomCheck");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {
			IChange change = (IChange) obj;
			ITable affectedTable = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<?> it = affectedTable.iterator();
			while (it.hasNext()) {
				IRow row = (IRow) it.next();
				if (!row.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
					IItem fatherItem = (IItem) row.getReferent();
					ITable bomTable = fatherItem.getTable(ItemConstants.TABLE_REDLINEBOM);
					Iterator<?> it2 = bomTable.iterator();
					MultiMap<IItem, String> map = new MultiValueMap<>();
					while (it2.hasNext()) {
						IRow row2 = (IRow) it2.next();
						if (!row2.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
							IItem childItem = (IItem) row2.getReferent();
							ICell group_Stat = row2.getCell(1636);
							ICell group_No = row2.getCell(1639);
							// System.out.println(childItem);
							String group_Stat_String = group_Stat.getValue().toString();
							String group_No_String = group_No.getValue().toString();
							if (map.containsKey(childItem)) {
								if (map.containsValue(group_Stat_String)) {
									if (group_Stat_String.equals("1"))
										result += "父階:" + fatherItem + "，其BOM 有重複[" + childItem + "]" + "\n";
								} else
									map.put(childItem, group_Stat_String);
							} else
								map.put(childItem, group_Stat_String);
							if (!check_NoIsNull_StatIs1(group_No_String, group_Stat_String)) {
								// System.out.println(
								// "父階:" + fatherItem.getName() + "，其子階:" +
								// childItem +
								// "，group_No為空，但group_Stat不為1");
								result += "父階:" + fatherItem.getName() + "，其子階:" + childItem
										+ "，group_No為空，但group_Stat不為1" + "\n";
							}
							if (has_group_no(group_No_String)) {
								mapp.put(group_No_String, group_Stat_String);
							}

							// System.out.println("A"+bom.size());

						}
					}
					// System.out.println("B" + map.size());
					// if (!isPartAndGroup_unique(bomTable, map)) {
					// result += "父階:" + fatherItem + "BOM 有重複" + "\n";
					// }
					Set<String> set = mapp.keySet();
					Iterator<String> it3 = set.iterator();
					while (it3.hasNext()) {
						String key = it3.next();
						Collection col = (Collection) mapp.get(key);
						if (!isGroupSizeOver1(col))
							// System.out.println("父階:"+fatherItem + "
							// Group_No:" +
							// key + "，至少需要兩筆以上");
							result += "父階:" + fatherItem + "，Group_No:" + key + "，至少需要兩筆以上" + "\n";
						if (!isGrouphasStat1(col))
							// System.out.println("父階:"+fatherItem + "
							// Group_No:" +
							// key + "，要有1筆 Stat=1");
							result += "父階:" + fatherItem + "，Group_No:" + key + "，要有1筆 Stat=1" + "\n";
						if (isGroupNo_duplicate(col)) {
							// System.out.println("父階:"+fatherItem + "
							// Group_No:" +
							// key + "，Stat重複");
							result += "父階:" + fatherItem + "，Group_No:" + key + "，Stat重複" + "\n";
						}
						if (isGroupNo_jump(col)) {
							// System.out.println("父階:"+fatherItem + "
							// Group_No:" +
							// key + "，有Stat跳號");
							result += "父階:" + fatherItem + "，Group_No:" + key + "，Stat跳號" + "\n";
						}

					}
					mapp.clear();
				}
			}
		} catch (

		Exception e) {
			log.setErrorMsg(e);

		} finally {
			log.updataDBLog();
			if (result.length() == 0) {
				try {
					ICell cell = obj.getCell(1562);
					// cell.setValue("Yes");
					html_header = HtmlFormat.Html_Header("請執行CheckBOM檢查程式", "WinSome(1573)", "執行成功", obj);
				} catch (APIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return new ActionResult(ActionResult.STRING, html_header);

			} else {
				try {
					html_header = HtmlFormat.Html_Header("請執行CheckBOM檢查程式", "WinSome(1573)", result, obj);
				} catch (APIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return new ActionResult(ActionResult.EXCEPTION, new Exception(html_header));

			}
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
	public boolean check_NoIsNull_StatIs1(String group_No, String group_Stat) throws APIException {
		if (group_No.equals("")) {
			if (group_Stat.equals("1"))
				return true;
			else
				return false;
		} else
			return true;
	}

	public boolean has_group_no(String group_No) { // Group是否為空
		if (!group_No.equals("")) {
			return true;
		} else
			return false;
	}

	public boolean isGroupSizeOver1(Collection col) {// 有Group，其數要大於1

		if (col.size() >= 2) {
			return true;
		} else
			return false;
	}

	public boolean isGrouphasStat1(Collection col) {// 是否有一筆Stat=1
		if (col.contains("1")) {
			return true;
		} else
			return false;
	}

	public boolean isGroupNo_duplicate(Collection col) {// 同GroupNo，Stat有無跳號
		Set<?> S = new HashSet<>(col);
		if (S.size() != col.size()) {

			return true;
		} else
			return false;
	}

	public boolean isGroupNo_jump(Collection col) { // 同GroupNo，Stat有無跳號
		int max = Integer.valueOf(Collections.max(col).toString());
		if (max != col.size()) {
			return true;
		} else
			return false;
	}

	// public boolean isPartAndGroup_unique(ITable bom, Map map) throws
	// APIException {
	// if ((bom.size() - Redline_Remove_BomSize(bom)) == map.size()) {
	// return true;
	// } else
	// return false;
	// }

	@Override
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo info) {
		Event_BomCheck def = new Event_BomCheck();
		IWFChangeStatusEventInfo Info = (IWFChangeStatusEventInfo) info;
		IChange change = null;
		try {
			change = (IChange) Info.getDataObject();
		} catch (APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new EventActionResult(info, def.doAction(session, node, change));
	}

	// public int Redline_Remove_BomSize(ITable Bom) throws APIException {
	// Iterator<?> it = Bom.iterator();
	// int count = 0;
	// while (it.hasNext()) {
	// IRow row = (IRow) it.next();
	// IItem item = (IItem) row.getReferent();
	// if (row.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
	// count++;
	// System.out.println(item);
	// }
	//
	// }
	// System.out.println(count);
	// return count;
	// }

}
