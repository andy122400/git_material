package pdm000;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.Adler32;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.accton.acctonRule;
import com.andy.plm.dblog.DBLogIt;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年8月17日 Description: program logic: 1.
 **********************************************************************/

public class Px_AutoApprover_set_ECN implements ICustomAction {

	public static void main(String[] args) throws APIException {
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		Px_AutoApprover_set_ECN aa = new Px_AutoApprover_set_ECN();
		IChange change = (IChange) session.getObject(IChange.OBJECT_TYPE, "ECN210100124");
		System.out.println(change);
		aa.doAction(session, null, change);
	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	IAgileSession session;
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2");
	DBLogIt log = new DBLogIt();
	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	IChange currentchange;
	IWorkflow currentworkflow;
	IStatus currentStatus;
	BufferedReader reader;
	String ChangeWorkflow;
	String ChangeStatus;
	String ApproveType = "";
	String Division = "";
	String cellID;
	String Condition_Filed;
	String Condition_Value;
	String Site_Filed;
	String Site_Value;
	// private Integer ECNchange_PageThree_IssueType=2479246;
	String user_logon;
	Statement stmt;
	ArrayList<IUser> approverArrayList = new ArrayList<IUser>();
	ArrayList<IUser> observerArrayList = new ArrayList<IUser>();
	Set<String> set1 = new HashSet<String>();
	Set<String> set2 = new HashSet<String>();
	Set<String> tempset = new HashSet<String>(
			Arrays.asList(new String[] { "139", "152", "153", "249", "161", "165", "170", "175", "249" }));
	ArrayList<IUser> defaultApproverList;
	String current_Site_Value;
	private boolean pass;

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
		log.setPgName("pdm000.Px_AutoApprover_set_ECN");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		log.dblog("程式:自動帶入ECN表單簽核人");
		this.session = session;
		try {
			stmt = msdb.createStatement();
			currentchange = (IChange) obj;
			currentworkflow = currentchange.getWorkflow(); // 取的目前表單的Workflow名稱
			System.out.println("當前流程名稱: " + currentworkflow.getName());
			log.dblog("表單當前流程名稱: " + currentworkflow.getName());
			currentStatus = currentchange.getStatus();// 取的目前表單的站別名稱
			System.out.println("當前站別名稱: " + currentStatus.getName());
			log.dblog("表單當前站別名稱: " + currentStatus.getName());
			getConfing("D:/Agile/Agile936/integration/sdk/extensions/Config_Approverby_WFECN.csv"); // 讀取設定檔，找到該表單流程暫別之參數檔
			readDataAndAddApprover();// ，讀取資料 加入簽核人員於List
			log.dblog("預計將帶入的簽核人員:[" + approverArrayList.toString() + "]");
			defaultApproverList = new ArrayList<IUser>(Arrays.asList(currentchange.getApprovers(currentStatus)));
			log.dblog("站別預設簽核人員:[" + defaultApproverList.toString() + "]");
			addapprover();// 針對list執行人員加簽
			if (approverArrayList.size() != 0 || pass) // 如果要將帶入的簽核人員不為空時
				removeAdmin();// 移除Admin管理員
			msdb.close(); // 關閉資料庫連線

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
		// ini = new Ini();
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
	/**********************************************************************
	 * getConfing 取得Confing檔案
	 ***************************************************************/
	public void getConfing(String fileLocation) throws IOException, APIException, NumberFormatException, SQLException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileLocation));// 檔案讀取路徑
		reader = new BufferedReader(isr);
	}

	/**********************************************************************
	 * readDataAndAddApprover 讀取資料，並針對資料填件進行Approver List人員新增
	 * 
	 * @throws Exception
	 ***************************************************************/
	public void readDataAndAddApprover() throws Exception {
		String line = null;
		while ((line = reader.readLine()) != null) {
			String field[] = line.split(",");
			/** 讀取 **/
			// System.out.print(ChangeWorkflow + "\t" + ChangeStatus + "\t");
			ChangeWorkflow = field[1].trim();
			ChangeStatus = field[2].trim();
			// System.out.println("A:"+field[1].trim()+field[2].trim()+field[4].trim());
			// System.out.println("B:"+Division);
			// System.out.print(ChangeWorkflow + "\t" + ChangeStatus + "\t");
			// System.out.println(currentworkflow.getName());
			// System.out.println();
			// if
			// (((!Division.equals(field[4].trim())&&!ApproveType.equals(field[3].trim()))))
			// { // 同一Division只要符合一次即可
			if (ChangeWorkflow.equals(currentworkflow.getName()) && ChangeStatus.equals(currentStatus.getName())) {// 找到符合站別與流程的設定檔資訊
				ApproveType = field[3].trim();
				Division = field[4].trim().replace("|", ",");
				cellID = field[5].trim();
				Condition_Filed = field[6].trim();
				Condition_Value = field[7].trim();
				// System.out.println("fvdc"+Condition_Value);
				String[] set1_array = Condition_Value.split(";");
				// System.out.println("set1_array:"+set1_array);
				set1 = new HashSet<String>(Arrays.asList(set1_array));
				// System.out.println("set1"+set1.toString());
				Site_Filed = field[8].trim();
				Site_Value = field[9].trim();
				System.out.println(ChangeWorkflow + "\t" + ChangeStatus + "\t" + ApproveType + "\t" + Division + "\t");
				log.dblog("設定檔資訊" + "\n" + "流程:[" + ChangeWorkflow + "]站別:[" + ChangeStatus + "]簽核類別:[" + ApproveType
						+ "]單位別:[" + Division + "]廠區:[" + Site_Value + "]");
				switch (ApproveType) {
				case "1":
					getApproversByAffectedItemWhereUse_F();
					break;
				case "2":
					getObersversByAffectedItemWhereUse_F();
					break;
				case "3":
					getApproversByChangeCell();
					break;
				case "6":
					getPMCApproversByAffectedItem_MFGSITE();
					break;
				case "7":
					getPMCObersersByAffectedItem_MFGSITE();
					break;
				case "8":
					getPAApproversByAffectedItem_MFGSITE();
					break;
				case "9":
					getPAObersersByAffectedItem_MFGSITE();
					break;
				}
			}
		}
	}

	/**********************************************************************
	 * Type1:getAffectedItemWhereUse_F_Approvers
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getApproversByAffectedItemWhereUse_F() throws Exception {
		if (!Site_Filed.equals("N/A")) {// 如果Site_Filed不等於N/A，代表有廠區
			current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();// 取得廠區資料Accton/Joytec/All
			if (current_Site_Value.equals(Site_Value)) {
				String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
				set2 = new HashSet<String>(Arrays.asList(set2_array));
				if (hasIntersection(set1, set2)) {
					Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
					String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString().replace(";", ",");
					ResultSet rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','"
							+ Change_FPartName + "','" + Division + "'");
					// ITable affectedtable =
					// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
					// if (affectedtable.size() == 0)
					// throw new Exception("受影響料號，不能為空");
					// Iterator<?> it = affectedtable.iterator();
					// String affectedItem = "";
					// while (it.hasNext()) {
					// IRow row = (IRow) it.next();
					//
					// if
					// (acctonRule.getAffectedItemType(row).equals("Phantom")) {
					// affectedItem +=
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
					// .toString() + ",";
					// }
					//
					// }
					// System.out.println("Aexec pdmdb.dbo.zp_agile_get_fpartno
					// '" + affectedItem + "'");
					// ResultSet rs = stmt.executeQuery("exec
					// pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
					// affectedItem = "";
					// while (rs.next()) {
					// affectedItem += rs.getString(1) + ",";
					// }
					//
					// System.out.println(affectedItem);
					// System.out.println("exec
					// [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
					// affectedItem + "','"
					// + Division + "'");
					// rs = stmt.executeQuery("exec
					// [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
					// affectedItem + "','"
					// + Division + "'");

					while (rs.next()) {
						// pmdl_pn = rs.getString(3);
						// pjm_type = rs.getString(4);
						// user_name = rs.getString(7);
						user_logon = rs.getString(7);
						// // System.out.println(pmdl_pn + " " + pjm_type +
						// " "
						// +
						// user_name
						// // + "
						// // " + user_logon);
						IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
						if (!approverArrayList.contains(user))
							approverArrayList.add(user);
						//
					}

				}
			}
		} else {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
				String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString().replace(";", ",");
				ResultSet rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");

				System.out.println(
						"AAAAQQQ exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
				// ITable affectedtable =
				// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				// if (affectedtable.size() == 0)
				// throw new Exception("受影響料號，不能為空");
				// Iterator<?> it = affectedtable.iterator();
				// String affectedItem = "";
				// while (it.hasNext()) {
				// IRow row = (IRow) it.next();
				//
				// if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
				// // String itemtype =
				// //
				// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
				// // if
				// // (acctonRule.getAffectedItemType(row).equals("Component"))
				// // {
				// affectedItem +=
				// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
				// .toString() + ",";
				// // }
				// }
				//
				// }
				// // System.out.println(affectedItem);
				// System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno '" +
				// affectedItem + "'");
				// ResultSet rs = stmt.executeQuery("exec
				// pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
				// affectedItem = "";
				// while (rs.next()) {
				// affectedItem += rs.getString(1) + ",";
				// }
				// log.dblog(affectedItem);
				// System.out.println(affectedItem);
				// System.out.println(
				// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
				// affectedItem + "','" + Division + "'");
				// rs = stmt.executeQuery(
				// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
				// affectedItem + "','" + Division + "'");
				// log.dblog("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','"
				// + affectedItem + "','" + Division
				// + "'".replace("'", "%"));
				while (rs.next()) {
					user_logon = rs.getString(7);
					;
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
					//
				}

			}
		}
		if (approverArrayList.size() == 0) {
			pass = true;
		}
	}

	private void getPAObersersByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				String childItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						IItem fatherItem = (IItem) row.getReferent();
						ITable RedlineTable = fatherItem.getTable(ItemConstants.TABLE_REDLINEBOM);
						ITwoWayIterator it2 = RedlineTable.getTableIterator();
						while (it2.hasNext()) {
							IRow redtablerow = (IRow) it2.next();
							if (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)
									|| redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)) {
								IItem item = (IItem) redtablerow.getReferent();
								if (acctonRule.getItemType(item).equals("Component")) {
									if (!isIgnoreParts(item)) {
										childItem += item + ",";
									}

								}

							}

						}
					}

				}
				System.out.println(childItem);
				String Stm = "";

				if (Site_Value.equals("Accton")) {
					Stm = ",'2'"; // 2為SP代號
					zp_buyer_Accton(Stm, childItem, observerArrayList);
				}

				else if (Site_Value.equals("JoyTech")) {
					Stm = ",'10'";// 10為SP代號
					zp_buyer_Joytech(Stm, childItem, observerArrayList);
				} else if (Site_Value.equals("All")) {
					Stm = "";
					zp_buyer_Accton(Stm, childItem, observerArrayList);
					zp_buyer_Joytech(Stm, childItem, observerArrayList);
				}

			}
		}
	}

	private void getPAApproversByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			// if
			// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
			// {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				String childItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					IItem fatherItem = (IItem) row.getReferent();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						// String itemtype =
						// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
						// affectedItem +=
						// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
						// + ",";

						ITable RedlineTable = fatherItem.getTable(ItemConstants.TABLE_REDLINEBOM);
						ITwoWayIterator it2 = RedlineTable.getTableIterator();
						while (it2.hasNext()) {
							IRow redtablerow = (IRow) it2.next();
							if (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)
									|| redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_ADDED)) {

								// 20201022 winsome說只要Remove的料
								// 20201214 winsome 改為有Redline就簽 不管移除 新增 變更
								// if
								// (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)
								// ||
								// redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED))
								// {
								IItem item = (IItem) redtablerow.getReferent();
								if (acctonRule.getItemType(item).equals("Component")) {
									if (!isIgnoreParts(item)) {
										childItem += item + ",";
									}
								}
							} // 20201214 winsome 改為有Redline就簽 不管移除 新增 變更
							else if (redtablerow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_MODIFIED)) { // modified
																										// 可能有透過替換的方法
								String aa = redtablerow.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER).getValue()
										.toString();
								IItem bb = (IItem) redtablerow.getReferent();

								if (!aa.equals(bb.getName())) {
									IItem item = (IItem) session.getObject(IItem.OBJECT_TYPE, aa);
									if (acctonRule.getItemType(item).equals("Component")) {
										if (!isIgnoreParts(item)) {
											childItem += item + ",";
										}
									}
									if (acctonRule.getItemType(bb).equals("Component")) {
										if (!isIgnoreParts(bb)) {
											childItem += bb + ",";
										}
									}
								}

							}
						}
					} else if (acctonRule.getAffectedItemType(row).equals("Component")) { // 20201214
																							// Running
																							// Change料
																							// 要給PA簽
						if (!isIgnoreParts(fatherItem)) {
							childItem += fatherItem + ",";
						}
					}
				}
				log.dblog("[Andy]:" + childItem);
				System.out.println(childItem);
				String Stm = "";

				if (Site_Value.equals("Accton")) {
					Stm = ",'2'"; // 2為SP代號
					zp_buyer_Accton(Stm, childItem, approverArrayList);
				}

				else if (Site_Value.equals("JoyTech")) {
					Stm = ",'10'";// 10為SP代號
					zp_buyer_Joytech(Stm, childItem, approverArrayList);
				} else if (Site_Value.equals("All")) {
					Stm = "";
					zp_buyer_Accton(Stm, childItem, approverArrayList);
					zp_buyer_Joytech(Stm, childItem, approverArrayList);
				}
				// System.out.println("zp_agile_part_buyer '" + childItem + "'"
				// + Stm);
				// ResultSet rs = stmt.executeQuery("zp_agile_part_buyer '" +
				// childItem + "'" + Stm);
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				//
				// //
				// if (!approverArrayList.contains(user))
				// approverArrayList.add(user);
				//
				// }

			}
		}
		log.dblog("規則八簽核人員:" + approverArrayList);
	}

	/**********************************************************************
	 * Type6:getPMCApproversByAffectedItem_MFGSITE
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getPMCApproversByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				// if
				// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
				// {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						// String itemtype =
						// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();

						affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
								.toString() + ",";
					}
				}
				// 取F階---Start
				System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
				affectedItem = "";
				while (rs.next()) {
					affectedItem += rs.getString(1) + ",";
				}
				// 取F階---END

				System.out.println(affectedItem);
				String Stm = "";

				if (Site_Value.equals("Accton")) {
					Stm = ",'2'"; // 2為SP代號
					zp_pmc_Accton(Stm, affectedItem, approverArrayList);

				} else if (Site_Value.equals("JoyTech")) {
					Stm = ",'10'";// 10為SP代號
					zp_pmc_Joytech(Stm, affectedItem, approverArrayList);
				} else if (Site_Value.equals("All")) {
					Stm = "";
					zp_pmc_Accton(Stm, affectedItem, approverArrayList);
					zp_pmc_Joytech(Stm, affectedItem, approverArrayList);
				}
				// System.out.println("zp_agile_product_planner '" +
				// affectedItem + "'" + Stm);
				// ResultSet rs = stmt.executeQuery("zp_agile_product_planner '"
				// + affectedItem + "'" + Stm);
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				//
				// //
				// if (!approverArrayList.contains(user))
				// approverArrayList.add(user);
				//
				// }
			}
		}
	}

	/**********************************************************************
	 * Type7:getPMCApproversByAffectedItem_MFGSITE
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getPMCObersersByAffectedItem_MFGSITE() throws Exception {
		String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
		if (current_Site_Value.equals(Site_Value)) {
			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				// if
				// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
				// {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				if (affectedtable.size() == 0)
					throw new Exception("受影響料號，不能為空");
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						// String itemtype =
						// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
						affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
								.toString() + ",";
					}
				}
				System.out.println(affectedItem);
				String Stm = "";

				if (Site_Value.equals("Accton")) {
					Stm = ",'2'"; // 2為SP代號
					zp_pmc_Accton(Stm, affectedItem, observerArrayList);

				} else if (Site_Value.equals("JoyTech")) {
					Stm = ",'10'";// 10為SP代號
					zp_pmc_Joytech(Stm, affectedItem, observerArrayList);
				} else if (Site_Value.equals("All")) {
					Stm = "";
					zp_pmc_Accton(Stm, affectedItem, observerArrayList);
					zp_pmc_Joytech(Stm, affectedItem, observerArrayList);
				}

				// if (Site_Value.equals("Accton"))
				// B = ",'2'";
				// else if (Site_Value.equals("JoyTech"))
				// B = ",'10'";
				// else if (Site_Value.equals("All"))
				// B = "";
				//
				// System.out.println("zp_agile_product_planner '" +
				// affectedItem + "'" + B);
				// ResultSet rs = stmt.executeQuery("zp_agile_product_planner '"
				// + affectedItem + "'" + B);
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				// if (!observerArrayList.contains(user))
				// observerArrayList.add(user);
				// //
				//
				// }
			}
		}
	}

	/**********************************************************************
	 * Type2:getAffectedItemWhereUse_F_Obersvers
	 * 
	 * @throws Exception
	 **********************************************************************/
	private void getObersversByAffectedItemWhereUse_F() throws Exception {
		if (!Site_Filed.equals("N/A")) {
			current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
			if (current_Site_Value.equals(Site_Value)) {
				String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
				set2 = new HashSet<String>(Arrays.asList(set2_array));
				if (hasIntersection(set1, set2)) {
					Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
					String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString().replace(";", ",");
					ResultSet rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','"
							+ Change_FPartName + "','" + Division + "'");
					// if
					// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
					// {
					// ITable affectedtable =
					// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
					// if (affectedtable.size() == 0)
					// throw new Exception("受影響料號，不能為空");
					// Iterator<?> it = affectedtable.iterator();
					// String affectedItem = "";
					// while (it.hasNext()) {
					// IRow row = (IRow) it.next();
					// // String itemtype =
					// //
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
					// // if
					// //
					// (acctonRule.getAffectedItemType(row).equals("Component"))
					// // {
					// if
					// (acctonRule.getAffectedItemType(row).equals("Phantom")) {
					// affectedItem +=
					// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
					// .toString() + ",";
					// // }
					// }
					// }
					// // System.out.println(affectedItem);
					// System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno
					// '" + affectedItem + "'");
					// ResultSet rs = stmt.executeQuery("exec
					// pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
					// affectedItem = "";
					// while (rs.next()) {
					// affectedItem += rs.getString(1) + ",";
					// }
					// System.out.println(affectedItem);
					// System.out.println(
					// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
					// affectedItem + "','" + Division + "'");
					// rs = stmt.executeQuery(
					// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
					// affectedItem + "','" + Division + "'");
					while (rs.next()) {
						// pmdl_pn = rs.getString(3);
						// pjm_type = rs.getString(4);
						// user_name = rs.getString(7);
						user_logon = rs.getString(7);
						// // System.out.println(pmdl_pn + " " + pjm_type + " "
						// +
						// user_name
						// // + "
						// // " + user_logon);
						IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
						if (!observerArrayList.contains(user))
							observerArrayList.add(user);
						//
					}
				}
			}

		} else {

			String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
			set2 = new HashSet<String>(Arrays.asList(set2_array));
			if (hasIntersection(set1, set2)) {
				Integer chnage_pagethree_FPartName = Integer.valueOf(1564);
				String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString().replace(";", ",");
				ResultSet rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
				// // if
				// //
				// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
				// // {
				// ITable affectedtable =
				// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				// if (affectedtable.size() == 0)
				// throw new Exception("受影響料號，不能為空");
				// Iterator<?> it = affectedtable.iterator();
				// String affectedItem = "";
				// while (it.hasNext()) {
				// IRow row = (IRow) it.next();
				// // String itemtype =
				// //
				// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
				// // if
				// // (acctonRule.getAffectedItemType(row).equals("Component"))
				// // {
				// if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
				// affectedItem +=
				// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
				// .toString() + ",";
				// // }
				// }
				// }
				// // System.out.println(affectedItem);
				//// if (!affectedItem.equals("")) {// 代表該表單有放虛階，其餘沒需皆則不用處理
				// System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno '" +
				// affectedItem + "'");
				// ResultSet rs = stmt.executeQuery("exec
				// pdmdb.dbo.zp_agile_get_fpartno '" + affectedItem + "'");
				// affectedItem = "";
				// while (rs.next()) {
				// affectedItem += rs.getString(1) + ",";
				// }
				// System.out.println(affectedItem);
				// String a = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','"
				// + affectedItem + "','" + Division
				// + "'";
				//
				// log.dblog(a.replace("'", "%"));
				// rs = stmt.executeQuery(
				// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
				// affectedItem + "','" + Division + "'");
				while (rs.next()) {
					// pmdl_pn = rs.getString(3);
					// pjm_type = rs.getString(4);
					// user_name = rs.getString(7);
					user_logon = rs.getString(7);
					// // System.out.println(pmdl_pn + " " + pjm_type + " "
					// +
					// user_name
					// // + "
					// // " + user_logon);
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!observerArrayList.contains(user))
						observerArrayList.add(user);
					//
				}

			}
		}
	}

	/**********************************************************************
	 * getApproversByChange
	 **********************************************************************/
	private void getApproversByChangeCell() throws NumberFormatException, APIException {
		String[] set2_array = currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().split(";");
		set2 = new HashSet<String>(Arrays.asList(set2_array));
		if (hasIntersection(set1, set2)) {
			// if
			// (currentchange.getCell(Integer.valueOf(Condition_Filed)).toString().contains(Condition_Value))
			// {
			ICell cell = currentchange.getCell(Integer.valueOf(cellID));
			IAgileList AgileList = (IAgileList) cell.getValue();
			IAgileList[] AgileList_array = AgileList.getSelection();
			// System.out.println(AgileList_array[0].getValue());
			for (IAgileList list : AgileList_array) {
				IUser users = (IUser) list.getValue();
				if (!approverArrayList.contains(users))
					approverArrayList.add(users);

			}

		}

	}

	/**********************************************************************
	 * addapprover
	 * 
	 **********************************************************************/
	public void addapprover() throws APIException {
		observerArrayList.removeAll(approverArrayList);// 去掉observer中與approver中重複之人員
		approverArrayList.removeAll(defaultApproverList);// 去掉approver中與預設簽核中重複之人員
		log.dblog("新增之Approver人員" + approverArrayList.toString());
		log.dblog("新增之observer人員" + observerArrayList.toString());
		currentchange.addReviewers(currentStatus, approverArrayList, observerArrayList, null, false, "");
	}

	/**********************************************************************
	 * removeAdmin
	 * 
	 **********************************************************************/
	@SuppressWarnings("deprecation")
	public void removeAdmin() throws APIException {

		IUser admin = (IUser) session.getObject(IUser.OBJECT_TYPE, "admin2");
		IUser[] approvers = new IUser[] { admin };

		currentchange.removeApprovers(currentStatus, approvers, null, "");

	}

	public boolean isIgnoreParts(IItem item) throws APIException {
		boolean results = false;
		String part_header = item.getCell(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).getValue().toString().split(" ")[0];
		System.out.println("part_header" + part_header);
		if (tempset.contains(part_header)) {
			System.out.println(item.getName() + "~於排除名單中");
			results = true;
		}
		return results;

	}

	public boolean hasIntersection(Set set1, Set set2) {
		int set1Size = set1.size();
		log.dblog(set1.toString());
		int set2Size = set2.size();
		log.dblog(set2.toString());
		set1.addAll(set2);
		int allSetSize = set1.size();
		log.dblog(set1.toString());
		if (allSetSize != (set1Size + set2Size))
			return true;
		else
			return false;
	}

	public void zp_buyer_Accton(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {
		System.out.println("zp_agile_part_buyer '" + childItem + "'" + Stm);

		ResultSet rs = stmt.executeQuery("zp_agile_part_buyer '" + childItem + "'" + Stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			System.out.println(user_logon);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);

			//
			if (!list.contains(user))
				list.add(user);

		}
	}

	public void zp_buyer_Joytech(String Stm, String childItem, ArrayList<IUser> list)
			throws SQLException, APIException {

		Connection joymsdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-10");
		Statement joystmt = joymsdb.createStatement();
		String stm = "zp_agile_part_buyer '" + childItem + "'" + Stm;
		System.out.println(stm);
		ResultSet rs = joystmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!list.contains(user)) // 判斷不重複
				list.add(user);
		}

	}

	public void zp_pmc_Accton(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {

		System.out.println("zp_agile_product_planner '" + childItem + "'" + Stm);
		ResultSet rs = stmt.executeQuery("zp_agile_product_planner '" + childItem + "'" + Stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			System.out.println(user_logon);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			//
			if (!list.contains(user))
				list.add(user);
		}
	}

	public void zp_pmc_Joytech(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {

		Connection joymsdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-10");
		Statement joystmt = joymsdb.createStatement();
		String stm = "zp_agile_product_planner '" + childItem + "'" + Stm;
		System.out.println(stm);
		ResultSet rs = joystmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!list.contains(user)) // 判斷不重複
				list.add(user);
		}

	}

}
