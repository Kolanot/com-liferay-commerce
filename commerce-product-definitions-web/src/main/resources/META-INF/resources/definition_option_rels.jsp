<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<%
CPDefinitionOptionRelDisplayContext cpDefinitionOptionRelDisplayContext = (CPDefinitionOptionRelDisplayContext)request.getAttribute(WebKeys.PORTLET_DISPLAY_CONTEXT);

CPDefinition cpDefinition = cpDefinitionOptionRelDisplayContext.getCPDefinition();
long cpDefinitionId = cpDefinitionOptionRelDisplayContext.getCPDefinitionId();
%>

<c:if test="<%= cpDefinitionOptionRelDisplayContext.hasViewPermission() %>">
	<portlet:resourceURL id="cpDefinitionOptionRels" var="cpDefinitionOptionsURL">
	</portlet:resourceURL>

	<liferay-portlet:renderURL var="cpDefinitionOptionRelURL" windowState="<%= LiferayWindowState.EXCLUSIVE.toString() %>">
		<portlet:param name="mvcRenderCommandName" value="editProductDefinitionOptionRel" />
	</liferay-portlet:renderURL>

	<portlet:resourceURL id="cpDefinitionOptionValueRels" var="cpDefinitionOptionValueRelsURL">
	</portlet:resourceURL>

	<portlet:actionURL name="editProductDefinitionOptionRel" var="editProductDefinitionOptionRelURL" />

	<liferay-portlet:renderURL var="cpDefinitionOptionValueRelURL" windowState="<%= LiferayWindowState.EXCLUSIVE.toString() %>">
		<portlet:param name="mvcRenderCommandName" value="editCPDefinitionOptionValueRel" />
	</liferay-portlet:renderURL>

	<%
	Map<String, Object> context = new HashMap<>();

	context.put("cpDefinitionId", String.valueOf(cpDefinitionId));
	context.put("cpDefinitionOptionsURL", cpDefinitionOptionsURL);
	context.put("cpDefinitionOptionValueRelsURL", cpDefinitionOptionValueRelsURL);
	context.put("cpDefinitionOptionValueRelURL", cpDefinitionOptionValueRelURL);
	context.put("editProductDefinitionOptionRelURL", editProductDefinitionOptionRelURL);
	context.put("hasEditPermission", cpDefinitionOptionRelDisplayContext.hasEditPermission());
	context.put("id", "CPDefinitionOptionsEditor");
	context.put("namespace", liferayPortletResponse.getNamespace());
	context.put("optionsItemSelectorURL", cpDefinitionOptionRelDisplayContext.getItemSelectorUrl());
	context.put("optionURL", cpDefinitionOptionRelURL);
	context.put("pathThemeImages", themeDisplay.getPathThemeImages());
	context.put("successMessage", LanguageUtil.get(request, "your-request-completed-successfully"));
	%>

	<div class="container-fluid-1280" id="<portlet:namespace />CPOptionsEditor">
		<soy:template-renderer
			context="<%= context %>"
			module="commerce-product-definitions-web@1.1.9/definition_option_rel/CPDefinitionOptionsEditor.es"
			templateNamespace="CPDefinitionOptionsEditor.render"
		/>
	</div>
</c:if>