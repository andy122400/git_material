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
import java.util.Iterator;

import javax.print.attribute.standard.MediaName;

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
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.accton.acctonRule;
import com.andy.plm.dblog.DBLogIt;

//import Dve.main_AutoApprover;

/*****************************
 * Information******************************** Editor:Andy_chuang Last
 * Modify:2020 2020年7月17日 Description: program logic: 1.
 **********************************************************************/

public class Px_AutoApprover_set implements ICustomAction {

	public static void main(String[] args) throws APIException, SQLException {

		Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2"); // missql
		stmt = msdb.createStatement();
		IAgileSession session = connector.agile.AgileSession_Connection.getAgileAdminSession();
		Px_AutoApprover_set aa = new Px_AutoApprover_set();
		IChange change = (IChange) session.getObject(IChange.OBJECT_TYPE, "DCN210200515");
		System.out.println(change);
		aa.doAction(session, null, change);
	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	Connection msdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2"); // missql
	DBLogIt log = new DBLogIt();
	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
	String confing_Loacation = "D:/Agile/Agile936/integration/sdk/extensions/Config_Approverby_WFFunction.csv";
	String error_Message = "";
	IChange currentchange;
	IWorkflow currentworkflow;
	IStatus currentStatus;
	IAgileSession session = null;
	String ChangeWorkflow, ChangeStatus, ApproveType, Division;
	String project_name;
	String pjm_type;
	String user_name;
	String user_logon;
	Integer chnage_pagethree_projectname;
	Integer user_pagetwo_managername = 1301;
	Integer change_affecteditem_projectname = 4611;
	Integer change_affecteditem_Document_Review_Type = 2000019235;
	ArrayList<IUser> approverArrayList = new ArrayList<IUser>();
	ArrayList<IUser> observerArrayList = new ArrayList<IUser>();
	String config_cellID;
	BufferedReader reader;
	String Condition_Filed;
	static Statement stmt;
	Integer CMPart_Pagethree_Material_Type = 1539;
	private String Condition_Value;
	private Integer chnage_pagethree_FPartName;
	private String pmdl_pn;
	private String Site_Filed;
	private String Site_Value;
	ArrayList<IUser> defaultApproverList;
	private boolean pass;
	private Integer chnage_pagethree_project;
	private ResultSet rs;

	/**********************************************************************
	 * 3.主程式(請勿修改位置)
	 **********************************************************************/
	@Override
	@SuppressWarnings("finally")
	public ActionResult doAction(IAgileSession session, INode node, IDataObject obj) {
		/**********************************************************************
		 * 3-1.dblog參數設定
		 **********************************************************************/
		log.setUserInformation(session);
		log.setPxInfo(obj);
		log.setPgName("pdm000.Px_AutoApprover_set");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/
		this.session = session;

		try {
			stmt = msdb.createStatement();
			currentchange = (IChange) obj;
			currentworkflow = currentchange.getWorkflow(); // 取的目前表單的Workflow名稱
			log.dblog(currentworkflow.toString());
			currentStatus = currentchange.getStatus();// 取的目前表單的站別名稱
			log.dblog(currentStatus.toString());
			getConfing(confing_Loacation); // 讀取設定檔，找到該表單流程暫別之參數檔
			readDataAndAddApprover();// ，讀取資料 加入簽核人員於List
			defaultApproverList = new ArrayList<IUser>(Arrays.asList(currentchange.getApprovers(currentStatus))); // 該站預設人員
			log.dblog("預設之Approver人員" + defaultApproverList.toString());
			Integer approverSize = approverArrayList.size();
			addApprover();// 針對list執行人員加簽
			if (approverSize != 0 || pass) // 如果有抓到加簽人員 現階段就可以PASS
				removeAdmin();// 移除Admin管理員
			msdb.close(); // 關閉資料庫連線
		} catch (Exception e) {
			e.printStackTrace();
			log.setErrorMsg(e);
		} finally {
			log.updataDBLog();
			return new ActionResult(ActionResult.STRING, log.getResult());
		}
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
	// session.close();
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 * 
	 * @throws SQLException
	 * @throws NumberFormatException
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
	 ***************************************************************/
	public void readDataAndAddApprover() throws NumberFormatException, IOException, APIException, SQLException {

		String line = null;
		while ((line = reader.readLine()) != null) {
			String field[] = line.split(",");
			/** 讀取 **/
			ChangeWorkflow = field[1].trim();
			ChangeStatus = field[2].trim();
			if (ChangeWorkflow.equals(currentworkflow.getName()) && ChangeStatus.equals(currentStatus.getName())) {
				ApproveType = field[3].trim();
				Division = field[4].trim().replace("|", ",");
				config_cellID = field[5].trim();
				Condition_Filed = field[6].trim();
				Condition_Value = field[7].trim();
				Site_Filed = field[8].trim();
				Site_Value = field[9].trim();
				System.out.println(ChangeWorkflow + "\t" + ChangeStatus + "\t" + ApproveType + "\t" + Division + "\t"
						+ Condition_Filed + "\t" + Condition_Value + "\t");

				switch (ApproveType) {
				case "1":
					log.dblog("進入Type1");
					getUserManager_Approver();
					break;
				case "2":
					getChangeCell_PA_Approver(); // 等待sp調整與測試
					break;
				case "3":
					addProjectApprovers();
					break;
				case "4":
					addFPartApprovers();

					break;
				case "5":
					addPart_WhereUse_F_Approver();
					break;
				case "6":

					addChangeCellApprovers();
					break;
				case "7":
					getAffectrdPart_Header();
					break;

				case "8":
					getAffectedItem_AE(); // 等待sp調整與測試
					break;
				case "9":
					addPart_WhereUse_PMC_Approver();
					break;
				case "10":
					getAffectrdComponents_Sourer(); // 等待sp調整與測試
					break;
				case "11":
					addPart_WhereUse_F_RoleorProject_Approver();
					break;
				case "12":
					getAffectPart_WhereUse_F_Approver();
					break;
				// case "13":// CAR
				// break;
				// case "14":// CAR
				// break;
				// case "15":// CAR
				// break;
				// case "16":
				// getAffectrdMfgSite_PMC(); // 等待sp調整與測試
				// break;
				// case "17":
				// getAffectPart_WhereUse_F_Observer();
				// break;

				case "18": // 20200902新增
					getAffectItemProjectID_Approvers();
					break;
				case "19": // 20200902新增
					getSecendSource_Approvers();
					break;
				case "20": // 20200923新增
					getApprovers_ModelPhaseOut();
					break;
				case "21": // 20200923新增
					getApprovers_ModelPhaseOut2();
					break;
				case "22": // 20200923新增
					getAffectPhantomPart_WhereUse_F_Approver();
					break;
				case "23": // 20201125新增
					getAffectPhantomPart_WhereUse_F_Approver_bySite();
					break;
				case "99": // 20201205新增
					getDocumentApprovers();
					break;

				}
			}

		}
	}

	/**********************************************************************
	 * Type1: 根據申請人主管欄位，取得簽核人員
	 **********************************************************************/
	public void getUserManager_Approver() throws NumberFormatException, APIException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			IUser user = (IUser) currentchange.getCell(Integer.valueOf(config_cellID)).getReferent(); // 取得表單中的Originator的User物件
			String managerName_String = user.getCell(user_pagetwo_managername).getValue().toString();// 取得主管的名稱
			IUser approve = (IUser) session.getObject(IUser.OBJECT_TYPE, managerName_String); // 取得主管的物件
			approverArrayList.add(approve); // 新增至簽核名單中
		}
	}

	/**********************************************************************
	 * Type2: 根據表單上的料號欄位的人員資料會簽-抓取DB對應兩地PA人員簽核
	 **********************************************************************/
	// public void getChangeCell_PA_Approver() throws APIException, SQLException
	// {
	// String current_Condition_Value = "";
	// if (!Condition_Filed.equals("N/A")) {
	// current_Condition_Value =
	// currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
	// }
	// if (Condition_Filed.equals("N/A") ||
	// current_Condition_Value.equals(Condition_Value)) {
	// chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
	// String Change_FPartName =
	// currentchange.getCell(chnage_pagethree_FPartName).getValue().toString();
	// Change_FPartName = Change_FPartName.replace(";", ",");
	// ResultSet rs = stmt.executeQuery(
	// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName +
	// "','" + Division + "'");
	// while (rs.next()) {
	// user_logon = rs.getString(2);
	// IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS,
	// user_logon);
	// if (!approverArrayList.contains(user)) //判斷不重複
	// approverArrayList.add(user);
	// }
	//
	// }
	// }

	public void getChangeCell_PA_Approver() throws APIException, SQLException {
		System.out.println(123);
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A")) {
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		}
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {

			ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<?> it = affectedtable.getReferentIterator();
			String affectedItem = "";
			while (it.hasNext()) {
				IItem item = (IItem) it.next();
				// String PartRule = item.getAgileClass().toString().split("
				// ")[0];
				// IItem PartRule_item = (IItem)
				// session.getObject(IItem.OBJECT_TYPE, PartRule);
				affectedItem += item.getName() + ",";
			}

			// chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
			// String Change_FPartName =
			// currentchange.getCell(chnage_pagethree_FPartName).getValue().toString();
			// Change_FPartName = Change_FPartName.replace(";", ",");
			String stm = "zp_agile_part_buyer '" + affectedItem + "'";
			System.out.println(stm);
			ResultSet rs = stmt.executeQuery(stm);
			while (rs.next()) {
				user_logon = rs.getString(3);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user)) // 判斷不重複
					approverArrayList.add(user);
			}
			// System.out.println("gg");

			Connection joymsdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-10");
			Statement joystmt = joymsdb.createStatement();
			System.out.println(stm);
			rs = joystmt.executeQuery(stm);
			while (rs.next()) {
				user_logon = rs.getString(3);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user)) // 判斷不重複
					approverArrayList.add(user);
			}

		}
	}

	/**********************************************************************
	 * Type3: 根據申請人主管欄位，取得簽核人員
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void addProjectApprovers() throws NumberFormatException, APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			chnage_pagethree_projectname = Integer.valueOf(config_cellID);
			String Change_project = currentchange.getCell(chnage_pagethree_projectname).getValue().toString();
			String sql = "";
			if (currentworkflow.toString().equals("Design Change Notice Workflow")) {
				sql = "exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" + Change_project + "','','','" + Division + "'";
			} else {
				sql = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'" + Change_project + "','','','" + Division + "'";
			}
			System.out.println(sql);
			// Statement stmt = msdb.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {

				pmdl_pn = rs.getString(3);
				pjm_type = rs.getString(4);
				user_name = rs.getString(6);
				user_logon = rs.getString(7);
				System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user))
					approverArrayList.add(user);
			}

		}

	}

	public void addProjectApprovers(String cellID) throws NumberFormatException, APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			chnage_pagethree_projectname = Integer.valueOf(cellID);
			String Change_project = currentchange.getCell(chnage_pagethree_projectname).getValue().toString();
			System.out.println(
					"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'" + Change_project + "','','','" + Division + "'");
			// Statement stmt = msdb.createStatement();
			ResultSet rs = stmt.executeQuery(
					"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'" + Change_project + "','','','" + Division + "'");
			;

			while (rs.next()) {

				pmdl_pn = rs.getString(3);
				pjm_type = rs.getString(4);
				user_name = rs.getString(6);
				user_logon = rs.getString(7);
				System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user))
					approverArrayList.add(user);
			}

		}

	}

	/**********************************************************************
	 * Type4: 根據表單上的成品料號欄位的人員資料會簽 (by Part)-會抓取DB對應單位人員簽核
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void addFPartApprovers() throws APIException, SQLException {
		Connection msdb2 = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-2"); // missql
		Statement stmt2 = msdb2.createStatement();
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			if (currentworkflow.getName().toString().equals("Initial Part Apply Workflow")
					|| currentworkflow.getName().toString().equals("New Part Apply Workflow")) {
				chnage_pagethree_project = Integer.valueOf(config_cellID);
				String Change_Project = currentchange.getCell(chnage_pagethree_project).getValue().toString()
						.replace(";", ",");
				rs = stmt2.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" + Change_Project + "','','','" + Division + "'");
				String a = "exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" + Change_Project + "','','','" + Division + "'";

			} else {
				chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
				String Change_FPartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString()
						.replace(";", ",");
				rs = stmt2.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
				String a = "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division
						+ "'";

			}

			// log.dblog(a.replace("'", "&"));

			while (rs.next()) {
				pmdl_pn = rs.getString(3);
				pjm_type = rs.getString(4);
				user_name = rs.getString(7);
				user_logon = rs.getString(7);
				System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user))
					approverArrayList.add(user);

			}
			msdb2.close();
		}
	}

	/**********************************************************************
	 * Type5: 根據表單上的成品料號欄位的人員資料會簽 ，逆展對應F階，並抓取對應之角色
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void addPart_WhereUse_F_Approver() throws APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A")) {
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		}
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
			String Change_PartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString();
			String Change_FPartName = "";
			// System.out.println("前"+Change_PartName);
			Change_PartName = Change_PartName.replace(";", ",").replace("-C", "");
			// System.out.println("後"+Change_PartName);
			// System.out.println("F");
			// Statement stmt = msdb.createStatement();
			System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + Change_PartName + "'");
			ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno  '" + Change_PartName + "'");
			while (rs.next()) {
				Change_FPartName += rs.getString(1) + ",";

			}
			System.out.println(
					"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
			rs = stmt.executeQuery(
					"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','" + Division + "'");
			while (rs.next()) {
				// pmdl_pn = rs.getString(3);
				// pjm_type = rs.getString(4);
				// user_name = rs.getString(7);
				user_logon = rs.getString(7);
				log.dblog(user_logon);
				// System.out.println(pmdl_pn + " " + pjm_type + " " + user_name
				// + " " + user_logon);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user))
					approverArrayList.add(user);
			}
		}

	}

	// msdb.close();

	/**********************************************************************
	 * Type6: 根據表單欄位，取得簽核人員
	 * 
	 * @return
	 **********************************************************************/
	public void addChangeCellApprovers() throws NumberFormatException, APIException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			// IUser approve = (IUser)
			// currentchange.getCell(Integer.valueOf(cellID)).getReferent(;。
			ICell cell = currentchange.getCell(Integer.valueOf(config_cellID));
			IAgileList AgileList = (IAgileList) cell.getValue();
			IAgileList[] AgileList_array = AgileList.getSelection();

			for (IAgileList list : AgileList_array) {
				IUser users = (IUser) list.getValue();
				if (!approverArrayList.contains(users))
					approverArrayList.add(users);

			}
			if (approverArrayList.size() == 0)
				removeAdmin();// 移除Admin管理員
			// approverArrayList.add(approve);
		}
		// return new IUser[] { approve };

	}

	/**********************************************************************
	 * Type7: 根據Affected Items料頭判斷誰簽、簽與不簽
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void getAffectrdPart_Header() throws APIException {
		String current_Condition_Value = "";
		log.dblog(currentStatus.getName());
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<?> it = affectedtable.iterator();
			while (it.hasNext()) {
				IRow row = (IRow) it.next();
				String itemtype = row.getReferent().getAgileClass().getName();// modify
																				// 20200226
																				// 因Request表單
																				// af_table沒有
																				// ite_type欄位
				System.out.println(itemtype);
				if (acctonRule.getAffectedItemType(row).equals("Component")) {
					IItem partrule = (IItem) session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemtype.split(" ")[0]);
					ICell cell = partrule.getCell(Integer.valueOf(config_cellID));
					IAgileList AgileList = (IAgileList) cell.getValue();
					IAgileList[] AgileList_array = AgileList.getSelection();
					// System.out.println(AgileList_array[0].getValue());
					for (IAgileList list : AgileList_array) {
						IUser users = (IUser) list.getValue();
						log.dblog(users.getName());
						if (!approverArrayList.contains(users))
							approverArrayList.add(users);

					}
				} else if (row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).getValue().toString()
						.equals("CM Part")) {
					// && currentchange.getWorkflow().getName().equals("Part
					// UnPhaseOut Apply Workflow")) { //0222拿掉
					IItem item = (IItem) row.getReferent();
					itemtype = item.getCell(CMPart_Pagethree_Material_Type).getValue().toString();
					IItem partrule = (IItem) session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemtype.split(":")[0]);
					ICell cell = partrule.getCell(Integer.valueOf(config_cellID));
					IAgileList AgileList = (IAgileList) cell.getValue();
					IAgileList[] AgileList_array = AgileList.getSelection();
					System.out.println(AgileList_array[0].getValue());
					for (IAgileList list : AgileList_array) {
						IUser users = (IUser) list.getValue();
						if (!approverArrayList.contains(users))
							approverArrayList.add(users);

					}
				} else {
				}
			}
		}
		log.dblog(approverArrayList.toString());
	}

	/**********************************************************************
	 * Type8: 依據Affected Item 之虛階料，對應抓取ERP 對應AE人員會簽
	 * 
	 * @throws APIException
	 * @throws NumberFormatException
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void getAffectedItem_AE() throws NumberFormatException, APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {

			ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<?> it = affectedtable.iterator();
			String affectedItem = "";
			while (it.hasNext()) {
				IRow row = (IRow) it.next();
				if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
					affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
							+ ",";
				}
			}
			System.out.println("zp_agile_part_ae '" + affectedItem + "'");
			ResultSet rs = stmt.executeQuery("zp_agile_part_ae '" + affectedItem + "'");
			while (rs.next()) {
				user_logon = rs.getString(1);
				IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
				if (!approverArrayList.contains(user))
					approverArrayList.add(user);
			}
			if (approverArrayList.size() == 0)
				removeAdmin();// 移除Admin管理員

		}
		log.dblog("規則八簽核人員:" + approverArrayList.toString());
	}

	// }

	/**********************************************************************
	 * Type9: 根據表單上的成品料號欄位的人員資料會簽 ，逆展對應F階，並抓取對應之pmc人員
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void addPart_WhereUse_PMC_Approver() throws APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
			log.dblog(current_Site_Value);
			log.dblog(Site_Value);
			if (current_Site_Value.equals(Site_Value)) {
				chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
				String Change_PartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString();
				// String Change_FPartName = "";
				Change_PartName = Change_PartName.replace(";", ",");
				// Statement stmt = msdb.createStatement();
				// ResultSet rs = stmt
				// .executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno
				// '"+Change_PartName +"'"); //要改
				// while (rs.next()) {
				// Change_FPartName+=rs.getString(1)+",";
				//
				// }
				log.dblog(Change_PartName);
				String Stm = "";
				if (Site_Value.equals("Accton")) {
					Stm = ",'2'"; // 2為SP代號
					zp_pmc_Accton(Stm, Change_PartName, approverArrayList);
				} else if (Site_Value.equals("JoyTech")) {
					Stm = ",'10'";// 10為SP代號
					zp_pmc_Joytech(Stm, Change_PartName, approverArrayList);
				} else if (Site_Value.equals("All")) {
					Stm = "";
					zp_pmc_Accton(Stm, Change_PartName, approverArrayList);
					zp_pmc_Joytech(Stm, Change_PartName, approverArrayList);
				}
				log.dblog(current_Condition_Value + "|" + current_Site_Value);
				// String aa ="exec zp_agile_product_planner '" +
				// Change_PartName + "'" + Stm;
				// log.dblog(aa.replace("'", "("));
				// System.out.println("exec zp_agile_product_planner '" +
				// Change_PartName + "'" + Stm);
				// ResultSet rs = stmt.executeQuery("exec
				// zp_agile_product_planner '" + Change_PartName + "'" + Stm);
				//// log.dblog(rs.getFetchSize());
				// while (rs.next()) {
				// user_logon = rs.getString(3);
				// log.dblog("A"+user_logon);
				// System.out.println(user_logon);
				// IUser user = (IUser)
				// session.getObject(UserConstants.CLASS_USERS_CLASS,
				// user_logon);
				// //
				// if (!approverArrayList.contains(user))
				// approverArrayList.add(user);
				// }
				log.dblog(approverArrayList.toString());

			}
		}
		// msdb.close();

	}

	/**********************************************************************
	 * Type10: 依據受影響料號之Components料 抓取對應之Buyer(PA)/Source from ERP
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void getAffectrdComponents_Sourer() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.iterator();
		String affectedItem = "";
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			// String itemtype =
			// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
			if (acctonRule.getAffectedItemType(row).equals("Component")
					|| acctonRule.getAffectedItemType(row).equals("CM Part")) {
				// IItem partrule = (IItem)
				// session.getObject(ItemConstants.CLASS_PARTS_CLASS,
				// row.getName().split(" ")[0]);
				affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString() + ",";
				// IUser user=(IUser)
				// partrule.getCell(Integer.valueOf(Field)).getReferent();

			}
			// }

		}
		System.out.println("exec zp_agile_part_sourcer '" + affectedItem + "'");
		ResultSet rs = stmt.executeQuery("exec zp_agile_part_sourcer '" + affectedItem + "'");
		while (rs.next()) {
			// pmdl_pn = rs.getString(3);
			// pjm_type = rs.getString(4);
			// user_name = rs.getString(7);
			user_logon = rs.getString(3);
			// System.out.println(pmdl_pn + " " + pjm_type + " " + user_name + "
			// " + user_logon);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!approverArrayList.contains(user))
				approverArrayList.add(user);

		}
	}

	/**********************************************************************
	 * Type11: 根據表單上的成品料號欄位的人員資料會簽 ，逆展對應F階，並抓取對應之角色,沒有的話就找Project規則
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void addPart_WhereUse_F_RoleorProject_Approver() throws APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			chnage_pagethree_FPartName = Integer.valueOf(config_cellID);
			String Change_PartName = currentchange.getCell(chnage_pagethree_FPartName).getValue().toString();
			// String Change_FPartName = null;
			String[] Change_PartName_array = Change_PartName.split(";");
			for (String Change_FPartName : Change_PartName_array) {
				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno  '" + Change_FPartName + "'");
				System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno  '" + Change_FPartName + "'");
				if (rs.next()) {
					Change_FPartName = rs.getString(1);
					rs = stmt.executeQuery("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + Change_FPartName + "','"
							+ Division + "'");
					while (rs.next()) {
						pmdl_pn = rs.getString(3);
						pjm_type = rs.getString(4);
						user_name = rs.getString(6);
						user_logon = rs.getString(7);
						System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
						IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
						if (!approverArrayList.contains(user))
							approverArrayList.add(user);
					}
				} else {

					addProjectApprovers("1540");

				}
			}
		}
	}

	/**********************************************************************
	 * Type12: 依據受影響料號之料 逆展至F階，並抓取對應的角色 (如果找不到F階，可以Pass到下一關 2020/11/13 mondify )
	 **********************************************************************/
	public void getAffectPart_WhereUse_F_Approver() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.iterator();
		String affectedItem = "";
		if (affectedtable.size() > 0) {
			while (it.hasNext()) {
				IRow row = (IRow) it.next();
				// if(acctonRule.getAffectedItemType(row).equals("Phantom")){
				affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString() + ",";
				// }

			}
			System.out.println("****************************" + affectedItem);
			ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
			affectedItem = "";
			while (rs.next()) {
				affectedItem += rs.getString(1) + ",";

			}
			if (affectedItem.equals("")) {
				pass = true;
			} else {
				log.dblog(affectedItem);

				System.out.println(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
				rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");

				while (rs.next()) {
					pmdl_pn = rs.getString(3);
					pjm_type = rs.getString(4);
					user_name = rs.getString(7);
					user_logon = rs.getString(7);
					System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
					if (user_name == null) {
						error_Message += pjm_type + "\t";
					}

					// if (user_name != null) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
					// }
				}
			}
		}
	}

	// /**********************************************************************
	// * Type16: 依據受影響料號， 抓取對應MfgSite之PMC人員 from ERP
	// *
	// * @throws SQLException
	// **********************************************************************/
	// public void getAffectrdMfgSite_PMC() throws APIException, SQLException {
	// ITable affectedtable =
	// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
	// Iterator<?> it = affectedtable.iterator();
	// String affectedItem = "";
	// while (it.hasNext()) {
	// IRow row = (IRow) it.next();
	// // String itemtype =
	// // row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
	// // if (acctonRule.getAffectedItemType(row).equals("Component")) {
	// // IItem partrule = (IItem)
	// // session.getObject(ItemConstants.CLASS_PARTS_CLASS,
	// // row.getName().split(" ")[0]);
	// affectedItem +=
	// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
	// + ",";
	// // IUser user=(IUser)
	// // partrule.getCell(Integer.valueOf(Field)).getReferent();
	//
	// // }
	// // }
	//
	// }
	//
	// ResultSet rs = stmt.executeQuery("待提供");
	// while (rs.next()) {
	// // pmdl_pn = rs.getString(3);
	// // pjm_type = rs.getString(4);
	// // user_name = rs.getString(7);
	// user_logon = rs.getString(2);
	// // System.out.println(pmdl_pn + " " + pjm_type + " " + user_name + "
	// // " + user_logon);
	// IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS,
	// user_logon);
	// if (!approverArrayList.contains(user))
	// approverArrayList.add(user);
	//
	// }
	// }

	// /**********************************************************************
	// * Type17: 依據受影響料號之料 逆展至F階，並抓取對應的角色
	// *
	// * @throws SQLException
	// **********************************************************************/
	// public void getAffectPart_WhereUse_F_Observer() throws APIException,
	// SQLException {
	// ITable affectedtable =
	// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
	// Iterator<?> it = affectedtable.iterator();
	// String affectedItem = "";
	// while (it.hasNext()) {
	// IRow row = (IRow) it.next();
	// ;
	// affectedItem +=
	// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue().toString()
	// + ",";
	// }
	// ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno '"
	// + affectedItem + "'");
	// while (rs.next()) {
	// affectedItem += rs.getString(1) + ",";
	// }
	// System.out.println("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" +
	// affectedItem + "','" + Division + "'");
	// rs = stmt.executeQuery(
	// "exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','"
	// + Division + "'");
	//
	// while (rs.next()) {
	// pmdl_pn = rs.getString(3);
	// pjm_type = rs.getString(4);
	// user_name = rs.getString(7);
	// user_logon = rs.getString(7);
	// System.out.println(pmdl_pn + " " + pjm_type + " " + user_name + " " +
	// user_logon);
	// IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS,
	// user_logon);
	// if (!observerArrayList.contains(user))
	// observerArrayList.add(user);
	//
	// }
	//
	// }

	/**********************************************************************
	 * Type18: 依據受影響料號之專案ID，並抓取對應的角色
	 **********************************************************************/
	public void getAffectItemProjectID_Approvers() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.iterator();
		String Item_project = "";
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			Item_project += row.getCell(change_affecteditem_projectname).getValue().toString() + ",";
		}
		String stm = "exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" + Item_project + "','','','" + Division + "'";
		System.out.println(stm);
		ResultSet rs = stmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(7);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!approverArrayList.contains(user))
				approverArrayList.add(user);

		}

	}

	/**********************************************************************
	 * Type19: 依據受影響料號之專案ID，並抓取對應的角色
	 **********************************************************************/
	public void getSecendSource_Approvers() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.getReferentIterator();
		String affectedItem = "";
		while (it.hasNext()) {
			IItem item = (IItem) it.next();
			String PartRule = item.getAgileClass().toString().split(" ")[0];
			IItem PartRule_item = (IItem) session.getObject(IItem.OBJECT_TYPE, PartRule);
			affectedItem += item.getName() + ",";
			if (!item.getAgileClass().toString().equals("CM Part")) {

				if (PartRule_item.getCell(1592).getValue().toString().equals("包材")) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, "melody");
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);

				} else if (PartRule_item.getCell(1592).getValue().toString().equals("機構")) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, "thf");
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
					// String json = "{\"name\":\"zitong\", \"age\":\"26\"}";
				}

			}

		}
		String stm = "zp_agile_part_sourcer '" + affectedItem + "'";
		System.out.println(stm);
		ResultSet rs = stmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!approverArrayList.contains(user))
				approverArrayList.add(user);

		}

		Connection joymsdb = connector.db.SqlServer_Connection.getMsDbConn("ERP-0-10");
		Statement joystmt = joymsdb.createStatement();
		System.out.println(stm);
		rs = joystmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
			if (!approverArrayList.contains(user)) // 判斷不重複
				approverArrayList.add(user);
		}

	}

	/**********************************************************************
	 * Type20: For Model Phase Out Apply Workflow 專用規則
	 * 
	 * @throws APIException
	 * @throws SQLException
	 **********************************************************************/
	private void getApprovers_ModelPhaseOut() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.getReferentIterator();
		String affectedItem = "";
		while (it.hasNext()) {
			IItem item = (IItem) it.next();
			// log.dblog(item.getAgileClass().toString());
			if (!item.getAgileClass().toString().equals("M I/O (Merchandise)")) {
				addChangeCellApprovers();
				break;
			}

		}

	}

	/**********************************************************************
	 * Type21: For Model Phase Out Apply Workflow 專用規則
	 * 
	 * @throws APIException
	 * @throws SQLException
	 **********************************************************************/
	private void getApprovers_ModelPhaseOut2() throws APIException, SQLException {
		// log.dblog("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.getReferentIterator();
		String affectedItem = "";
		while (it.hasNext()) {
			IItem item = (IItem) it.next();
			// log.dblog(item.getAgileClass().toString());
			if (item.getAgileClass().toString().equals("M I/O (Merchandise)")) {
				affectedItem = item.toString();
				System.out.println(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
				ResultSet rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");

				boolean tag = true;
				while (rs.next()) {
					tag = false;
					pmdl_pn = rs.getString(3);
					pjm_type = rs.getString(4);
					user_name = rs.getString(7);
					user_logon = rs.getString(7);
					System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);

					// if (user_name != null) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
					// }
				}
				if (tag) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, "misaki_chiou");
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
				}

			}
		}
		// System.out.println(affectedItem);
		// log.dblog(affectedItem);
		// System.out.println("exec pdmdb.dbo.zp_agile_get_fpartno '" +
		// affectedItem + "'");
		// ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno
		// '" + affectedItem + "'");
		// affectedItem="";
		// while (rs.next()) {
		// affectedItem += rs.getString(1) + ",";
		//
		// }

	}

	// public void getAffectrdPhantonAndChild_Header() throws APIException {
	// ITable affectedtable =
	// currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS); // 取得受影響料號表
	// Iterator<?> it = affectedtable.iterator();
	// while (it.hasNext()) { // 逐一讀取
	// IRow row = (IRow) it.next();
	// String itemtype =
	// row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString(); //
	// 取得itemtype
	// if (acctonRule.getAffectedItemType(row).equals("Phanton")) { //
	// 根據itemtype判斷類型(虛階、專案、原料)
	// IItem partrule = (IItem)
	// session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemtype);
	// // IUser user=(IUser)
	// // partrule.getCell(Integer.valueOf(Field)).getReferent();
	// ICell cell = partrule.getCell(Integer.valueOf(cellID));
	// IAgileList AgileList = (IAgileList) cell.getValue();
	// IAgileList[] AgileList_array = AgileList.getSelection();
	// // System.out.println(AgileList_array[0].getValue();
	// for (IAgileList list : AgileList_array) {
	// IUser users = (IUser) list.getValue();
	// approverArrayList.add(users);
	// }
	//
	// // 針對RedlineBom新增/修改/刪除的子階料件進行簽核人員判斷
	//
	// }
	//
	// }
	// }
	/**********************************************************************
	 * Type22: 依據受影響料號之虛階料 逆展至F階，並抓取對應的角色
	 **********************************************************************/
	public void getAffectPhantomPart_WhereUse_F_Approver() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.iterator();
		String affectedItem = "";
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			if (affectedtable.size() > 0) {
				while (it.hasNext()) {
					IRow row = (IRow) it.next();
					if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
						affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
								.toString() + ",";
					}

				}
				System.out.println("****************************" + affectedItem);
				log.dblog("[" + affectedItem + "]");

				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
				affectedItem = "";
				while (rs.next()) {
					affectedItem += rs.getString(1) + ",";
				}

				log.dblog("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
				log.dblog("exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
				rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");

				while (rs.next()) {
					pmdl_pn = rs.getString(3);
					pjm_type = rs.getString(4);
					user_name = rs.getString(7);
					user_logon = rs.getString(7);
					System.out.println(pmdl_pn + "  " + pjm_type + "  " + user_name + "  " + user_logon);
					if (user_name == null) {
						error_Message += pjm_type + "\t";
					}

					// if (user_name != null) {
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
					// }
				}
			}
		}
	}

	/**********************************************************************
	 * Type23: 根據表單上的成品料號欄位的人員資料會簽 ，逆展對應F階，並抓取對應之pmc人員
	 * 
	 * @throws SQLException
	 **********************************************************************/
	public void getAffectPhantomPart_WhereUse_F_Approver_bySite() throws APIException, SQLException {
		String current_Condition_Value = "";
		if (!Condition_Filed.equals("N/A"))
			current_Condition_Value = currentchange.getCell(Integer.valueOf(Condition_Filed)).getValue().toString();
		if (Condition_Filed.equals("N/A") || current_Condition_Value.equals(Condition_Value)) {
			String current_Site_Value = currentchange.getCell(Integer.valueOf(Site_Filed)).getValue().toString();
			if (current_Site_Value.equals(Site_Value)) {
				ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
				Iterator<?> it = affectedtable.iterator();
				String affectedItem = "";
				if (affectedtable.size() > 0) {
					while (it.hasNext()) {
						IRow row = (IRow) it.next();
						if (acctonRule.getAffectedItemType(row).equals("Phantom")) {
							affectedItem += row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_NUMBER).getValue()
									.toString() + ",";
						}

					}
				}

				ResultSet rs = stmt.executeQuery("exec pdmdb.dbo.zp_agile_get_fpartno  '" + affectedItem + "'");
				while (rs.next()) {
					affectedItem += rs.getString(1) + ",";
				}

				System.out.println(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");
				rs = stmt.executeQuery(
						"exec [PDMDB].[dbo].[zp_PMS_get_member] 1,'','','" + affectedItem + "','" + Division + "'");

				while (rs.next()) {
					user_logon = rs.getString(7);
					System.out.println(user_logon);
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					//
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);

				}
			}
		}

		// msdb.close();

	}

	/**********************************************************************
	 * Type99: 依據受影響料號之專案ID，並抓取對應的角色
	 **********************************************************************/
	public void getDocumentApprovers() throws APIException, SQLException {
		ITable affectedtable = currentchange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<?> it = affectedtable.iterator();
		// String Document_Review_Type = "";
		while (it.hasNext()) {
			IRow row = (IRow) it.next();
			String Document_Review_Type = row.getCell(change_affecteditem_Document_Review_Type).getValue().toString();
			String Document_Project = ((IItem) row.getReferent()).getCell(2021).getValue().toString();
			String Document_Review_Role = row.getCell(2000019265).getValue().toString();
			Document_Review_Role = Document_Review_Role.replace(";", ",");
			if (Document_Review_Type.equals("Manager")) {
				getUserManager_Approver();
			} else if (Document_Review_Type.equals("Team Member")) {
				String stm = "exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" + Document_Project + "','','','"
						+ Document_Review_Role + "'";
				log.dblog(stm.replace("'", "%"));
				System.out.println(stm);
				ResultSet rs = stmt.executeQuery(stm);
				while (rs.next()) {
					user_logon = rs.getString(7);
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
					if (!approverArrayList.contains(user))
						approverArrayList.add(user);
				}
			}

		}
		// String stm = "exec [PDMDB].[dbo].[zp_PMS_get_member] 0,'" +
		// Document_Review_Type + "','','','" + Division + "'";
		// System.out.println(stm);
		// ResultSet rs = stmt.executeQuery(stm);
		// while (rs.next()) {
		// user_logon = rs.getString(7);
		// IUser user = (IUser)
		// session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
		// if (!approverArrayList.contains(user))
		// approverArrayList.add(user);
		//
		// }
	}

	/**********************************************************************
	 * addapprover
	 **********************************************************************/
	public void addApprover() throws APIException {

		observerArrayList.removeAll(approverArrayList);// 去掉observer中與approver中重複之人員
		approverArrayList.removeAll(defaultApproverList);// 去掉approver中與預設簽核中重複之人員
		log.dblog("新增之Approver人員" + approverArrayList.toString());
		log.dblog("新增之observer人員" + observerArrayList.toString());
		currentchange.addReviewers(currentStatus, approverArrayList, observerArrayList, null, false, "");
	}

	/**********************************************************************
	 * removeAdmin
	 **********************************************************************/
	public void removeAdmin() throws APIException {
		ArrayList<IUser> adminApprover = new ArrayList<IUser>();
		IUser admin = (IUser) session.getObject(IUser.OBJECT_TYPE, "admin");
		adminApprover.add(admin);
		log.dblog("移除之Approver人員" + adminApprover.toString());
		currentchange.removeReviewers(currentStatus, adminApprover, null, null, "");
	}

	public void zp_pmc_Accton(String Stm, String childItem, ArrayList<IUser> list) throws SQLException, APIException {
		String stm = "zp_agile_product_planner '" + childItem + "'" + Stm;
		ResultSet rs = stmt.executeQuery(stm);
		while (rs.next()) {
			user_logon = rs.getString(3);
			System.out.println(user_logon);
			IUser user = (IUser) session.getObject(UserConstants.CLASS_USERS_CLASS, user_logon);
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
