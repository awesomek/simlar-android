/**
 * Copyright (C) 2013 - 2014 The Simlar Authors.
 *
 * This file is part of Simlar. (https://www.simlar.org)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.simlar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public final class ContactsListFragment extends android.support.v4.app.ListFragment
{
	private static final String LOGTAG = ContactsListFragment.class.getSimpleName();

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.main_activity_contact_list_no_contacts_found));
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState)
	{
		final ListView listView = getListView();

		if (listView == null) {
			Lg.e(LOGTAG, "no list view");
			return;
		}

		listView.setDivider(null);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id)
	{
		final String simlarId = ((ContactsAdapter) getListAdapter()).getItem(position).simlarId;
		if (Util.isNullOrEmpty(simlarId)) {
			Lg.e(LOGTAG, "onListItemClick: no simlarId found");
			return;
		}

		Lg.i(LOGTAG, "starting CallActivity with simlarId=", new Lg.Anonymizer(simlarId));
		startActivity(new Intent(getActivity(), CallActivity.class)
				.putExtra(CallActivity.INTENT_EXTRA_SIMLAR_ID, simlarId)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}
}
