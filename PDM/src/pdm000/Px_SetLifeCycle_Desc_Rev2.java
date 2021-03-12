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
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.dblog.DBLogIt;
import com.sun.jmx.snmp.SnmpUnknownAccContrModelException;
import com.sun.org.apache.bcel.internal.generic.AALOAD;

import sun.rmi.runtime.Log;

/***************************************************************
 * Editor:Andy_chuang LastModify:2020 2020年7月15日 Description:PX_自動生命週期、版本、描述前綴詞
 * logic: 1.取得表單資訊 2.取得對應參數資訊 3.循環受影響料號表 4.進行生命週期、版本、描述前綴詞 寫入 Remark:
 * 1.描述變更流程，請要確保描述裡面，沒有(!、*、?、9* )字符 2.文件類型 生命週期統一>>>Released 版本>>001 3.專案類型
 * 生命週期統一>>>ProjectID 版本>>001 4.OEM Part Apply Workflow 生命週期另外程式執行
 * 
 * 
 **********************************************************************/

public class Px_SetLifeCycle_Desc_Rev2 implements ICustomAction {

	public static void main(String[] args) throws APIException {

		Px_SetLifeCycle_Desc_Rev2 aa = new Px_SetLifeCycle_Desc_Rev2();
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		IChange obj = (IChange) session.getObject(IChange.OBJECT_TYPE, "DIF210100001");
		ITable table = obj.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = table.iterator();

		String output = null, old_PartRev, old_BomRev;
		int i = 0;
		while (it.hasNext()) {
			i++;
			System.out.println();
			IRow row = (IRow) it.next();
			ICell old_rev_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV);
			IItem item = (IItem) row.getReferent();
//			System.out.println("料號" + item);
			//// if (old_rev_cell.getValue().toString().length()==7){
			//
			// String old_PartRev1 =
			//// old_rev_cell.getValue().toString().split("-")[0];
			// String old_BomRev1 =
			//// old_rev_cell.getValue().toString().split("-")[1];
			// output = addrev_Normal(old_PartRev1) + "-" + old_BomRev1;
			//

			////
			// }else{
			if (item.getCell(2022).getValue().toString().equals("0:Normal")) {
				if (old_rev_cell.getValue().toString().length() == 0)
					output = "001-1";
				else {
					String old_PartRev1 = old_rev_cell.getValue().toString().split("-")[0];
					String old_BomRev1 = old_rev_cell.getValue().toString().split("-")[1];
					output = addrev_Normal(old_PartRev1) + "-" +  Integer.valueOf(old_BomRev1);
				}

			} else if (item.getCell(2022).getValue().toString().equals("1:RD")) {
				if (old_rev_cell.getValue().toString().length() == 0)
					output = "001-001";
				else {
					String old_PartRev1 = old_rev_cell.getValue().toString().split("-")[0];
					String old_BomRev1 = old_rev_cell.getValue().toString().split("-")[1];
					if(old_BomRev1.length()<3)
						old_BomRev1="0"+old_BomRev1;
					if(old_BomRev1.length()<3)
						old_BomRev1="0"+old_BomRev1;
					output = addrev_Normal(old_PartRev1) + "-" +  old_BomRev1;
				}
			}

			// }
			// String old_PartRev1 =
			// old_rev_cell.getValue().toString().split("-")[0];
			// String old_BomRev1 =
			// old_rev_cell.getValue().toString().split("-")[1];
			// // rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// // addrev_ECN(old_BomRev));
			// output = addrev_Normal(old_PartRev1) + "-" + old_BomRev1;
			// }

			//
			// if (old_rev_cell.getValue().toString().length() == 3) // 006
			// // rev_cell.setValue(addrev_Normal(old_rev_cell));
			// output = addrev_Normal(old_rev_cell);
			// else if
			// (item.getCell(2022).getValue().toString().equals("0:Normal")) {
			// old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			// old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// output = addrev_Normal(old_PartRev) + "-" +
			// addrev_ECN(old_BomRev);
			// // rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// // addrev_ECN(old_BomRev));
			// } else if
			// (item.getCell(2022).getValue().toString().equals("1:RD")) { //
			// 007-002
			// old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			// old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// output = addrev_Normal(old_PartRev) + "-" +
			// addrev_Normal(old_BomRev);
			// // rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// // addrev_Normal(old_BomRev));
			// }

			row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).setValue(output);
			System.out.println();
			 System.out.println(item.getCell(2022).getValue().toString()+"|"+old_rev_cell.getValue().toString()+"|"+output);

		}

		// aa.doAction(session, null, obj);
		// session.close();

	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	DBLogIt log = new DBLogIt(); // 寫Log到資料庫
	IAgileSession session = null;
	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	String changeworkflow, status, lifecycle, desc, rev, roolback_desc, FileID, List, Enable, SNumber;
	IWorkflow workflow;
	ICell lifecycle_cell, old_lifecycle_cell, rev_cell, old_rev_cell, description_cell, old_description_cell;
	IItem item;
	IStatus statu;
	IChange currentchange;
	Integer parts_PageTherr_oldLifecyclephase = 1301;
	String new_lifecycle;
	Map rows = new HashMap();
	// public static void main(String[] args) throws APIException {
	// Px_SetLifeCycle_Desc_Rev aa = new Px_SetLifeCycle_Desc_Rev();
	// IAgileSession session =
	// connector.agile.AgileSession_Connection.getAgileAdminSession();
	// IChange obj = (IChange) session.getObject(IChange.OBJECT_TYPE,
	// "PCR201000011");
	// aa.doAction(session, null, obj);
	// session.close();
	//
	// }

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
		log.setPgName("pdm000.Px_SetLifeCycle_Desc_Rev");// 自行定義程式名稱
		log.dblog("程式開始時間:" + getCurrentDateTime());
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {
			currentchange = (IChange) obj;
			log.dblog("表單名稱:" + currentchange.getName());
			workflow = currentchange.getWorkflow();
			log.dblog("流程名稱:" + workflow.getName());
			statu = currentchange.getStatus();
			log.dblog("站別名稱:" + statu.getName());
			getConfing("D:/Agile/Agile936/integration/sdk/extensions/Config_AutoLifeCycle_Desc_Rev.csv");
			// // 讀取設定檔，找到該表單流程別之參數
			// getConfing("D:/Config_AutoLifeCycle_Desc_Rev.csv");
			log.dblog("讀取設定檔 成功");
			log.dblog("開始針對受影響料號進行生命週期、版本、描述變更");
			ITable affectedTable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			log.dblog("受影響料號數料:" + affectedTable.size() + "個");
			Iterator<?> it = affectedTable.iterator();
			if (currentchange.getWorkflow().toString().equals("OEM Part Apply Workflow")) {
				Px_SetLifeCycle_Desc_Rev_OEM oem = new Px_SetLifeCycle_Desc_Rev_OEM();
				oem.doAction(session, node, obj);
			} else if (currentchange.getWorkflow().toString().equals("Phantom Part Request Workflow")
					&& currentchange.getCell(1539).getValue().toString().equals("4.買進賣出料(M/R料)")) {
				Px_SetLifeCycle_Desc_Rev_PPR ppr = new Px_SetLifeCycle_Desc_Rev_PPR();
				ppr.doAction(session, node, obj);
			} else {
				while (it.hasNext()) {
					Map<Integer, Object> mapx = new HashMap();
					IRow row = (IRow) it.next();
					item = (IItem) row.getReferent();
					System.out.println("料號:" + item.getName());
					// 取得affectedTable中的lifecycle、rev、description的新舊Cell，後續讀取比對\寫入使用。
					getAffectItem_Rev_LifeCycle_Description_Cell(row);
					// 8/13 新增需求 不分流程，遇到文件類的料件，版本統一為Released
					if (item.getAgileClass().getSuperClass().toString().equals("Documents")) {
						lifecycle_cell.setValue("Released");
						if (old_rev_cell.getValue().toString().equals("")) // 如果舊生命週期為空，001
							// rev_cell.setValue("001");
							mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, "001");
						else
							// rev_cell.setValue(addrev_Normal(old_rev_cell));
							mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, addrev_Normal(old_rev_cell));

					} else if (item.getAgileClass().toString().equals("ProjectID")) {// 8/21
																						// 新增需求
																						// 不分流成，遇到專案類的料件，版本統一為ProjectID
						// lifecycle_cell.setValue("ProjectID");
						mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE, "ProjectID");
						if (old_rev_cell.getValue().toString().equals("")) // 如果舊生命週期為空，001
							// rev_cell.setValue("001");
							mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, "001");
						else
							// rev_cell.setValue(addrev_Normal(old_rev_cell));
							mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, addrev_Normal(old_rev_cell));
					} else { // 其他料件類型就走設定檔規則

						mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV, SetRevesion());

						mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE, SetLifeCycle());
//						mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION, SetDesc());
						// }
					}
					rows.put(row, mapx);
				}

				affectedTable.updateRows(rows);
				rows.clear();
				ArrayList<Map<?, ?>> addPartList = new ArrayList();
			}
			// affectedTable.updateRows();

		} catch (Exception e) {
			e.printStackTrace();
			log.setErrorMsg(e);
		} finally {
			log.updataDBLog();
			// log=null;
			return new ActionResult(ActionResult.STRING, log.getResult());
		}

	}

	private void getAffectItem_Rev_LifeCycle_Description_Cell(IRow row) throws APIException {
		lifecycle_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE); //
		old_lifecycle_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE);
		rev_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV);
		old_rev_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV);
		description_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION);
		old_description_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_ITEM_DESCRIPTION);
	}

	/**********************************************************************
	 * 4.初始化
	 **********************************************************************/
	// private void init() {
	// // ini = new Ini();
	// }

	/**********************************************************************
	 * 5.解構&釋放
	 **********************************************************************/
	// private void close() {
	// try {
	// // adminSession.close();
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/
	/**********************************************************************
	 * 讀取設定檔，並根據表單、站別，找到對應之參數
	 **********************************************************************/
	@SuppressWarnings("resource")
	public void getConfing(String fileLocation) throws IOException, APIException {

		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileLocation));// 檔案讀取路徑
		BufferedReader reader = new BufferedReader(isr);
		String line = null;
		String FileValue = "";

		while ((line = reader.readLine()) != null) {
			String field[] = line.split(",");
			/** 讀取 **/
			SNumber = field[0].trim();
			changeworkflow = field[1].trim();
			status = field[5].trim();
			FileID = field[3].trim();
			List = field[4].trim();
			Enable = field[9].trim();
			log.dblog("1");
			if (changeworkflow.equals(workflow.getName()) && Enable.equals("Y")) {
				// if (changeworkflow.equals(workflow.getName()) &&
				// status.equals(statu.getName()) && Enable.equals("Y")) {
				log.dblog("1");
				if (!(FileID.equals("N/A"))) {
					FileValue = currentchange.getCell(Integer.valueOf(FileID)).toString();
					log.dblog("2");
				}
				System.out.println(List);
				if (List.equals(FileValue) || FileID.equals("N/A")) {
					log.dblog("3");
					lifecycle = field[6].trim();
					desc = field[7].trim();
					rev = field[8].trim();

					log.dblog("編號:" + SNumber + "\t" + changeworkflow + "\t" + status + "\t" + lifecycle + "\t" + desc
							+ "\t" + rev + "\n");
					break;
				}
			}
		}

	}

	public void getConfing_ken(String fileLocation) throws IOException, APIException {

		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileLocation));// 檔案讀取路徑
		BufferedReader reader = new BufferedReader(isr);
		String line = null;
		String FileValue = "";
		while ((line = reader.readLine()) != null) {

			String field[] = line.split(",");
			/** 讀取 **/
			SNumber = field[0].trim();
			changeworkflow = field[1].trim();
			status = field[5].trim();
			FileID = field[3].trim();
			List = field[4].trim();
			Enable = field[9].trim();
			if (changeworkflow.equals(workflow.getName())) {
				if (!(FileID.equals("N/A")))
					FileValue = currentchange.getCell(Integer.valueOf(FileID)).toString();
				if (List.equals(FileValue) || FileID.equals("N/A")) {
					lifecycle = field[6].trim();
					desc = field[7].trim();
					rev = field[8].trim();
					log.dblog("編號:" + SNumber + "\t" + changeworkflow + "\t" + status + "\t" + lifecycle + "\t" + desc
							+ "\t" + rev + "\n");
					break;
				}
			}
		}

	}

	/**********************************************************************
	 * SetRevesion 寫入生命週期
	 **********************************************************************/
	public String SetLifeCycle() throws APIException {
		String output = "";
		switch (lifecycle) {
		case "Rollback":
			log.dblog("Type:Rollback");
			// Rollback(item);
			Rollback_new(item);
			// lifecycle_cell.setValue(new_lifecycle);
			output = new_lifecycle;
			break;
		case "不變":
			new_lifecycle = old_lifecycle_cell.getValue().toString();
			// lifecycle_cell.setValue(new_lifecycle);
			output = new_lifecycle;
			break;
		default:
			// lifecycle_cell.setValue(lifecycle);
			output = lifecycle;
			break;
		}
		return output;
	}

	// /**********************************************************************
	// * SaveLifeCycleToParts(要寫入的料件、欄位、值) 保存鎖料前之生命週期於料件上
	// **********************************************************************/
	// public void SaveLifeCycleToParts(IItem item, Integer cellbaseID, String
	// value) throws APIException {
	// ICell cell = item.getCell(cellbaseID);
	// cell.setValue(value);
	// }

	/**********************************************************************
	 * SetRevesion 寫入版本
	 **********************************************************************/
	public String SetRevesion() throws APIException {
		String output = "";
		switch (rev) {

		case "Normal":
			if (old_rev_cell.getValue().toString().equals("")) // 如果舊生命週期為空，001
				// rev_cell.setValue("001");
				output = "001";
			else
				// rev_cell.setValue(addrev_Normal(old_rev_cell));
				output = addrev_Normal(old_rev_cell);
			break;
		case "DCN":
			if (old_rev_cell.getValue().toString().length() == 3) // 虛階初建BOM
				// rev_cell.setValue(addrev_Normal(old_rev_cell) + "-001");
				output = addrev_Normal(old_rev_cell) + "-001";
			else {
				String old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
				String old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
				// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
				// addrev_Normal(old_BomRev));
				output = addrev_Normal(old_PartRev) + "-" + addrev_Normal(old_BomRev);
			}
			break;
		case "ECN":
			String old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			String old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// addrev_ECN(old_BomRev));
			output = addrev_Normal(old_PartRev) + "-" + addrev_ECN(old_BomRev);
			break;
		case "Add_Pre":
			old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// addrev_ECN(old_BomRev));
			output = addrev_Normal(old_PartRev) + "-" + old_BomRev;
			break;
		case "Add1":
			if (old_rev_cell.getValue().toString().length() == 3) // 006
				// rev_cell.setValue(addrev_Normal(old_rev_cell));
				output = addrev_Normal(old_rev_cell);
			else if (item.getCell(2022).getValue().toString().equals("0:Normal")) {
				old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
				old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
				output = addrev_Normal(old_PartRev) + "-" + addrev_ECN(old_BomRev);
				// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
				// addrev_ECN(old_BomRev));
			} else if (item.getCell(2022).getValue().toString().equals("1:RD")) { // 007-002
				old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
				old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
				output = addrev_Normal(old_PartRev) + "-" + addrev_Normal(old_BomRev);
				// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
				// addrev_Normal(old_BomRev));
			}

			// System.out.println(e.);

			// if (old_rev_cell.getValue().toString().length() == 7) { //
			// 007-002
			// old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			// old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// addrev_Normal(old_BomRev));
			// } else if (old_rev_cell.getValue().toString().length() == 3) //
			// 006
			// rev_cell.setValue(addrev_Normal(old_rev_cell));
			// else if (old_rev_cell.getValue().toString().length() == 5) { //
			// 008-4
			// old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			// old_BomRev = old_rev_cell.getValue().toString().split("-")[1];
			// rev_cell.setValue(addrev_Normal(old_PartRev) + "-" +
			// addrev_ECN(old_BomRev));
			// }
			break;
		case "E2M":
			old_PartRev = old_rev_cell.getValue().toString().split("-")[0];
			// rev_cell.setValue(addrev_Normal(old_PartRev) + "-1");
			output = addrev_Normal(old_PartRev) + "-1";
			break;

		}
		return output;
	}

	/**********************************************************************
	 * addrev_Componet 針對Componet 將舊版本+1 Ex: 001>002>003
	 **********************************************************************/
	public static String addrev_Normal(ICell old_rev_cell) throws APIException {

		String old_rev = old_rev_cell.getValue().toString();// 取得001
		old_rev = (Integer.valueOf(old_rev) + 1001) + "";// +1
		old_rev = old_rev.substring(1, 4);
		return old_rev;

	}

	/**********************************************************************
	 * addrev_Componet 針對Componet 將舊版本+1 Ex: 001>002>003
	 **********************************************************************/
	public static String addrev_Normal(String old_rev) throws APIException {

		// String old_rev = old_rev_cell.getValue().toString();// 取得001
		old_rev = (Integer.valueOf(old_rev) + 1001) + "";// +1
		old_rev = old_rev.substring(1, 4);
		return old_rev;

	}

	/**********************************************************************
	 * addrev_ECN 針對ECN料件 將舊版本+1 Ex: E001>E002>E003
	 **********************************************************************/
	public String addrev_ECN(ICell old_rev_cell) throws APIException {

		String old_rev = old_rev_cell.getValue().toString(); // 取得E001的後三碼001
		old_rev = (Integer.valueOf(old_rev) + 1) + ""; // +1
		return old_rev;

	}

	public static String addrev_ECN(String old_rev) throws APIException {

		// String old_rev = old_rev_cell.getValue().toString(); // 取得E001的後三碼001
		old_rev = (Integer.valueOf(old_rev) + 1) + ""; // +1
		return old_rev;

	}

	/**********************************************************************
	 * addrev_E2M 針對EBOM轉為MBOM後 將舊版本+1 Ex: 1>2>3
	 **********************************************************************/
	public String addrev_E2M(ICell old_rev_cell) throws APIException {

		String old_rev = old_rev_cell.getValue().toString();// 取得1、2、3
		old_rev = (Integer.valueOf(old_rev) + 1) + "";// +1
		return old_rev;

	}

	/**********************************************************************
	 * SetDesc() 寫入描述
	 **********************************************************************/
	public String SetDesc() throws APIException {
		String output = "";
		log.dblog(desc);
		String[] descs = desc.split(" "); // descs =add !! / add 9* / remove !?
		switch (descs[0]) {
		case "Add":
			if (workflow.getName().toString().equals("Part PhaseOut Apply Workflow")) {
				// description_cell.setValue(descs[1] + old_description_cell);//
				// Modify20200904
				System.out.println((old_description_cell.getValue().toString()));
				if (old_description_cell.getValue().toString().startsWith("*"))// Modify20201113
					output = descs[1] + RemoveWord(old_description_cell);
				else
					output = descs[1] + old_description_cell;

			} else
				// description_cell.setValue(descs[1] +
				// RemoveWord(description_cell));// Modify20200904
				output = descs[1] + RemoveWord(description_cell);
			break;
		case "Remove":
			// description_cell.setValue(RemoveWord(description_cell));//
			// Modify20200904
			output = RemoveWord(description_cell);
			break;
		case "Rollback":
			// Rollback(item);
			Rollback_new(item);
			// description_cell.setValue(roolback_desc);
			output = roolback_desc;
			break;
		case "不變":
			String oldDesc = old_description_cell.getValue().toString();

			output = description_cell.getValue().toString();
			break;

		}
		return output;
	}

	/**********************************************************************
	 * RemoveWord 針對Cell內的值統一去除(9*、?、!、*)
	 **********************************************************************/
	public String RemoveWord(ICell old_description_cell) throws APIException {
		// log.dblog("A"+old_description_cell.getValue().toString());
		// log.dblog("B"+old_description_cell.getValue().toString().replace("9*",
		// "").replace("?", "").replace("!", "")
		// .replace("*", "").trim());
		return old_description_cell.getValue().toString().replace("9*", "").replace("?", "").replace("!", "")
				.replace("*", "").trim();
	}

	/**********************************************************************
	 * Rollback(IItem item) 取上上一版Item資訊(生命週期、描述)
	 * 
	 **********************************************************************/
	public void Rollback(IItem item) throws APIException {
		ITable table = item.getTable(ItemConstants.TABLE_CHANGEHISTORY);
		Iterator<?> it = table.iterator();
		it.next();
		IRow row = (IRow) it.next();
		ICell CHANGE_HISTORY_NUMBER_cell = row.getCell(ItemConstants.ATT_CHANGE_HISTORY_NUMBER);
		item.setRevision(CHANGE_HISTORY_NUMBER_cell.getValue());
		new_lifecycle = item.getCell(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).getValue().toString();
		roolback_desc = item.getCell(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION).toString();
	}

	public void Rollback_new(IItem item) throws APIException { // 20201105
																// Modify
		if (currentchange.getWorkflow().toString().equals("Project Pending Application Workflow")) {
			new_lifecycle = "05:Developing(??)";
			roolback_desc = "??" + RemoveWord(description_cell);
		} else if (currentchange.getWorkflow().toString().equals("Part Lifecycle Apply Workflow")) {
			new_lifecycle = "15:Standard()";
			roolback_desc = RemoveWord(description_cell);
		} else if (currentchange.getWorkflow().toString().equals("Part UnPhaseOut Apply Workflow")) {
			// new_lifecycle = "15:Standard()";
			if (old_description_cell.getValue().toString().startsWith("*??")) {
				roolback_desc = "??" + RemoveWord(old_description_cell);
				new_lifecycle = "Initial (??)";
			} else if (old_description_cell.getValue().toString().startsWith("**??")) {
				roolback_desc = "??" + RemoveWord(old_description_cell);
				new_lifecycle = "Initial (??)";
			} else if (old_description_cell.getValue().toString().startsWith("*?!")) {
				roolback_desc = "?!" + RemoveWord(old_description_cell);
				new_lifecycle = "Cond-Approved(?!)";
			} else if (old_description_cell.getValue().toString().startsWith("**?!")) {
				roolback_desc = "?!" + RemoveWord(old_description_cell);
				new_lifecycle = "Cond-Approved(?!)";
			} else if (old_description_cell.getValue().toString().startsWith("9*??")) {
				roolback_desc = "??" + RemoveWord(old_description_cell);
				new_lifecycle = "Initial (??)";
			} else if (old_description_cell.getValue().toString().startsWith("9*?!")) {
				roolback_desc = "?!" + RemoveWord(old_description_cell);
				new_lifecycle = "Cond-Approved(?!)";
			} else if (old_description_cell.getValue().toString().startsWith("9*")) {
				roolback_desc = RemoveWord(old_description_cell);
				new_lifecycle = "Approved()";
			} else if (old_description_cell.getValue().toString().startsWith("*")) {
				roolback_desc = RemoveWord(old_description_cell);
				new_lifecycle = "Approved()";
			} else {
				new_lifecycle = null;
			}
		}
	}

	public ActionResult doAction_ken(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		// log.setUserInformation(session);
		// log.setPxInfo(obj);
		// log.setPgName("Px_SetLifeCycle_Desc_Rev");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;
		try {
			currentchange = (IChange) obj;
			log.dblog("表單名稱:" + currentchange.getName());
			workflow = currentchange.getWorkflow();
			log.dblog("流程名稱:" + workflow.getName());
			statu = currentchange.getStatus();
			log.dblog("站別名稱:" + statu.getName());
			getConfing_ken("D:/Agile/Agile936/integration/sdk/extensions/Config_AutoLifeCycle_Desc_Rev.csv"); // 讀取設定檔，找到該表單流程別之參數
			log.dblog("讀取設定檔 成功");
			log.dblog("開始針對受影響料號進行生命週期、版本、描述變更");
			ITable affectedTable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			log.dblog("受影響料號數料:" + affectedTable.size() + "個");
			Iterator<?> it = affectedTable.iterator();
			if (currentchange.getWorkflow().toString().equals("Phantom Part Request Workflow")
					&& currentchange.getCell(1539).getValue().toString().equals("4.買進賣出料(M/R料)")) {
				Px_SetLifeCycle_Desc_Rev_PPR ppr = new Px_SetLifeCycle_Desc_Rev_PPR();
				ppr.doAction(session, node, obj);
			} else {
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					Map<Integer, Object> mapx = new HashMap();
					item = (IItem) row.getReferent();
					log.dblog("料號:" + item.getName());
					System.out.println("料號:" + item.getName());
					// 取得affectedTable中的lifecycle、rev、description的新舊Cell，後續讀取比對\寫入使用。
					getAffectItem_Rev_LifeCycle_Description_Cell(row);
					// 8/13 新增需求 不分流程，遇到文件類的料件，版本統一為Released
					// if
					// (item.getAgileClass().getSuperClass().toString().equals("Documents"))
					// {
					// lifecycle_cell.setValue("Released");
					// if (old_rev_cell.getValue().toString().equals("")) //
					// 如果舊生命週期為空，001
					// rev_cell.setValue("001");
					// else
					// rev_cell.setValue(addrev_Normal(old_rev_cell));
					//
					// } else if
					// (item.getAgileClass().toString().equals("ProjectID")) {//
					// 8/21
					// // 新增需求
					// // 不分流成，遇到專案類的料件，版本統一為ProjectID
					// lifecycle_cell.setValue("ProjectID");
					// if (old_rev_cell.getValue().toString().equals("")) //
					// 如果舊生命週期為空，001
					// rev_cell.setValue("001");
					// else
					// rev_cell.setValue(addrev_Normal(old_rev_cell));
					// } else { // 其他料件類型就走設定檔規則
					// if (!currentchange.getWorkflow().toString().equals("OEM
					// Part
					// Apply Workflow"))
					// SetLifeCycle();
					// SetDesc();

					mapx.put(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION, SetDesc());
					rows.put(row, mapx);
				}
			}
			affectedTable.updateRows(rows);
			// SetRevesion();
			//
			// }
			log.dblog("程式結束時間:" + getCurrentDateTime());
		} catch (Exception e) {
			log.setErrorMsg(e);
		} finally {

			log.updataDBLog();
			return new ActionResult(ActionResult.STRING, log.getResult());
		}

	}

	static private String getCurrentDateTime() {
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		sdFormat.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		Date date = new Date();
		String strDate = sdFormat.format(date);
		return strDate;
	}

	public String getPrefix(String desc) {
		String[] desc_array = desc.split("");
		for (int i = desc_array.length - 1; i >= 0; i--) {
			if (desc_array[i].equals("!") || desc_array[i].equals("*") || desc_array[i].equals("?")) {
				String Prefix = desc.substring(0, i + 1);
				return Prefix;
			}
		}
		return "";
	}
}
