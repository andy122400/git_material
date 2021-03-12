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

import sun.print.resources.serviceui_es;

/****************************
 * Information*********************************。 Editor:Andy_chuang Last
 * Modify:2020 2020年9月7日 Description: program logic: 1.
 **********************************************************************/

public class Px_SetLifeCycle_Desc_Rev_OEM implements ICustomAction{
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
		log.setPgName("pdm000.Px_SetLifeCycle_Desc_Rev_OEM");// 自行定義程式名稱
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
				ICell description_cell = row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_DESCRIPTION);
				String LifecyclePhases = (String) item.getCell(1303).getValue();
				row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).setValue(LifecyclePhases);
				row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).setValue("001");
//				if (LifecyclePhases.equals("Approved()")) {
//					description_cell.setValue(RemoveWord(description_cell));// Modify20200904
//				} else if (LifecyclePhases.equals("Initial (??)")) {
//					description_cell.setValue("??" + RemoveWord(description_cell));// Modify20200904
//				} else if (LifecyclePhases.equals("Initial (??)")) {
//					description_cell.setValue("??" + RemoveWord(description_cell));// Modify20200904
//				}
			}

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
}
