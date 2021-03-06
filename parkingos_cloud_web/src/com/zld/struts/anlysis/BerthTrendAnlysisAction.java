package com.zld.struts.anlysis;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BerthTrendAnlysisAction extends Action {
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private CommonMethods commonMethods;
	@Override
	public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest request,HttpServletResponse response) throws Exception{
		String action = RequestUtil.processParams(request, "action");
		Long uin = (Long)request.getSession().getAttribute("loginuin");//登录的用户id
		request.setAttribute("authid", request.getParameter("authid"));
		Long cityid = (Long)request.getSession().getAttribute("cityid");
		Long groupid = (Long)request.getSession().getAttribute("groupid");
		if(uin == null){
			response.sendRedirect("login.do");
			return null;
		}
		if(cityid == null && groupid == null){
			return null;
		}
		if(cityid == null) cityid = -1L;
		if(groupid == null) groupid = -1L;
		if(action.equals("")){
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Long today = TimeTools.getToDayBeginTime();
			request.setAttribute("btime", df2.format(today*1000));
			request.setAttribute("etime",  df2.format(System.currentTimeMillis()));
			//***********************获取车场泊位总数*****************************//
			Long asum = 0L;
			Long usum = 0L;
			Map<String, Object> map = commonMethods.getBerthCount(groupid, cityid);
			if(map != null){
				asum = (Long)map.get("asum");
				usum = (Long)map.get("usum");
			}
			request.setAttribute("asum", asum);
			request.setAttribute("usum", usum);
			//***********************end*****************************//
			return mapping.findForward("trend");
		}else if(action.equals("querytrend")){
			String sql = "select sum(used_count) usum,sum(month_used_count) msum,sum(time_used_count) tsum,create_time from park_anlysis_tb " +
					"where create_time between ? and ? ";
			List<Map<String, Object>> list = null;
			String btime = RequestUtil.processParams(request, "btime");
			String etime = RequestUtil.processParams(request, "etime");
			Long b = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime);
			Long e = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime);
			List<Object> params = new ArrayList<Object>();
			params.add(b);
			params.add(e);
			List<Object> parks = null;
			if(cityid > 0){
				parks = commonMethods.getparks(cityid);
			}else if(groupid > 0){
				parks = commonMethods.getParks(groupid);
			}
			if(parks != null && !parks.isEmpty()){
				String preParams  ="";
				for(Object parkid : parks){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " and comid in ("+preParams+") ";
				params.addAll(parks);
				sql += "group by create_time order by create_time ";
				list = pgOnlyReadService.getAllMap(sql, params);
			}
			if(list != null && !list.isEmpty()){
				for(int i=0;i<list.size();i++){
					Map<String, Object> map = (Map<String, Object>)list.get(i);
					Long create_time = (Long)map.get("create_time");
					map.put("time",TimeTools.getTime_yyyyMMdd_HHmm(create_time*1000));
				}
			}
			String json = StringUtils.createJson(list);
			AjaxUtil.ajaxOutput(response, json);
		}
		return null;
	}
}
