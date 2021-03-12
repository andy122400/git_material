package pdm000;

import java.util.Iterator;

import org.omg.PortableInterceptor.AdapterStateHelper;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.andy.plm.dblog.DBLogIt;

import connector.agile.AgileSession_Connection;
import sun.print.resources.serviceui_es;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年9月7日 Description: program logic: 1.
 **********************************************************************/

public class Px_SetLifeCycle_Desc_Rev_PPR implements ICustomAction {

//	public static void main(String[] args) throws APIException {
//		IAgileSession session = AgileSession_Connection.getAgileAdminSession();
//
//		Px_SetLifeCycle_Desc_Rev_PPR aa = new Px_SetLifeCycle_Desc_Rev_PPR();
//
//		IChange obj = (IChange) session.getObject(IChange.OBJECT_TYPE, "PPR201100028");
//		aa.doAction(session, null, obj);
//	}

	/**********************************************************************
	 * 1.標準變數宣告(必要)
	 **********************************************************************/
	DBLogIt log = new DBLogIt();

	/**********************************************************************
	 * 2.其他全域變數宣告
	 **********************************************************************/
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
		log.setPgName("pdm000.Px_SetLifeCycle_Desc_Rev_PPR");// 自行定義程式名稱
		/**********************************************************************
		 * 3-2.doAction
		 **********************************************************************/

		try {

			IChange Change = (IChange) obj;

			ITable affectedTable = Change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			Iterator<?> it = affectedTable.iterator();
			while (it.hasNext()) {
				IRow row = (IRow) it.next();
				IItem item = (IItem) row.getReferent();
				System.out.println(item + "QAZ");
				String item_lifeCycle = item.getCell(2482184).getValue().toString();
				ICell Description = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION);
				ICell old_rev_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV);
				String New_rev;
				if (old_rev_cell.getValue().toString().equals("")) // 如果舊生命週期為空，001
					New_rev = "001";
				else
					New_rev = addrev_Normal(old_rev_cell);

				if (item_lifeCycle.equals("15")) {
					row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).setValue("15:Standard()");
					row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION).setValue(RemoveWord(Description));

				} else if (item_lifeCycle.equals("05")) {
					row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).setValue("05:Developing(??)");
					row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION)
							.setValue("??" + RemoveWord(Description));
				}
				row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).setValue(New_rev);
				ICell description_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION);
			}

		} catch (Exception e) {
			log.setErrorMsg(e);
			e.printStackTrace();
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
	// } catch (Exception e) {
	// }
	// }

	/**********************************************************************
	 * 6.以下為Biz Function
	 **********************************************************************/
	/**********************************************************************
	 * RemoveWord 針對Cell內的值統一去除(9*、?、!、*)
	 **********************************************************************/
	public String RemoveWord(ICell old_description_cell) throws APIException {
		return old_description_cell.getValue().toString().replace("9*", "").replace("?", "").replace("!", "")
				.replace("*", "").trim();
	}

	/**********************************************************************
	 * addrev_Componet 針對Componet 將舊版本+1 Ex: 001>002>003
	 **********************************************************************/
	public String addrev_Normal(ICell old_rev_cell) throws APIException {

		String old_rev = old_rev_cell.getValue().toString();// 取得001
		old_rev = (Integer.valueOf(old_rev) + 1001) + "";// +1
		old_rev = old_rev.substring(1, 4);
		return old_rev;

	}

}
