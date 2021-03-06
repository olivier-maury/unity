/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import java.util.LinkedList;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;

import io.imunity.webelements.menu.MenuButton;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * Base for all views which contains subviews. Implements
 * {@link SubViewSwitcher} so updates breadcrumbs when subview is changed.
 * 
 * @author P.Piernik
 *
 */
public abstract class ViewWithSubViewBase extends CustomComponent implements SubViewSwitcher, UnityViewWithSubViews

{
	private LinkedList<UnitySubView> subViews;
	private Component mainView;
	private BreadcrumbsComponent breadCrumbs;

	public ViewWithSubViewBase()
	{
		subViews = new LinkedList<>();
		breadCrumbs = new BreadcrumbsComponent();
		breadCrumbs.setMargin(false);
	}

	protected void setMainView(Component mainView)
	{
		this.mainView = mainView;
		setCompositionRoot(mainView);
	}

	@Override
	public void exitSubView()
	{
		subViews.pollLast();
		if (subViews.isEmpty())
		{
			setCompositionRoot(mainView);
		} else
		{
			setCompositionRoot(subViews.getLast());
		}
		refreshBreadCrumbs();
	}

	@Override
	public void goToSubView(UnitySubView subview)
	{
		subViews.add(subview);
		setCompositionRoot(subview);
		refreshBreadCrumbs();
	}

	private void refreshBreadCrumbs()
	{
		breadCrumbs.removeAllComponents();
		for (UnitySubView subView : subViews)
		{
			subView.getBredcrumbs().forEach(b -> {
				breadCrumbs.addSeparator();
				breadCrumbs.addComponent(MenuButton.get(b).withCaption(b));
			});
		}
	}

	@Override
	public BreadcrumbsComponent getBreadcrumbsComponent()
	{
		return breadCrumbs;
	}
}
