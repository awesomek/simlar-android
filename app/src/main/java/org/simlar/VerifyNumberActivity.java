/**
 * Copyright (C) 2013 The Simlar Authors.
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

import java.util.Comparator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public final class VerifyNumberActivity extends ActionBarActivity
{
	private static final String LOGTAG = VerifyNumberActivity.class.getSimpleName();
	private static final int RESULT_CREATE_ACCOUNT_ACTIVITY = 0;

	private Spinner mSpinner;
	private EditText mEditNumber;
	private Button mButtonAccept;

	private final class EditNumberTextWatcher implements TextWatcher
	{
		public EditNumberTextWatcher()
		{
			super();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			VerifyNumberActivity.this.updateButtonAccept();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void afterTextChanged(Editable s)
		{
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		Lg.i(LOGTAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_verify_number);

		final int regionCode = SimlarNumber.readRegionCodeFromSimCardOrConfiguration(this);
		final String number = SimlarNumber.readLocalPhoneNumberFromSimCard(this);

		final ArrayAdapter<Integer> adapter = createCountryCodeSelector();
		mSpinner = (Spinner) findViewById(R.id.spinnerCountryCodes);
		mSpinner.setAdapter(adapter);

		Lg.i(LOGTAG, "proposing country code: ", regionCode);
		if (regionCode > 0) {
			mSpinner.setSelection(adapter.getPosition(regionCode));
		}

		// telephone number
		mEditNumber = (EditText) findViewById(R.id.editTextPhoneNumber);
		if (!Util.isNullOrEmpty(number)) {
			mEditNumber.setText(number);

			final TextView text = (TextView) findViewById(R.id.textViewCheckOrVerifyYourNumber);
			text.setText(getString(R.string.verify_number_activity_verify_your_number));
		} else {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run()
				{
					showSoftInputForEditNumber();
				}
			}, 100);
		}
		mEditNumber.addTextChangedListener(new EditNumberTextWatcher());

		mButtonAccept = (Button) findViewById(R.id.buttonRegister);
		updateButtonAccept();
	}

	private ArrayAdapter<Integer> createCountryCodeSelector()
	{
		final ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			adapter.addAll(SimlarNumber.getSupportedCountryCodes());
		} else {
			for (final Integer countryCode : SimlarNumber.getSupportedCountryCodes()) {
				adapter.add(countryCode);
			}
		}
		adapter.sort(new Comparator<Integer>() {
			@Override
			public int compare(final Integer lhs, final Integer rhs)
			{
				return lhs.compareTo(rhs);
			}
		});

		return adapter;
	}

	void showSoftInputForEditNumber()
	{
		Lg.e(LOGTAG, "no number");
		mEditNumber.requestFocus();
		if (((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
				.showSoftInput(mEditNumber, InputMethodManager.SHOW_IMPLICIT))
		{
			Lg.w(LOGTAG, "showSoftInput success");
		} else {
			Lg.w(LOGTAG, "showSoftInput failed");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		return true;
	}

	@Override
	protected void onResume()
	{
		Lg.i(LOGTAG, "onResume");
		super.onResume();

		if (PreferencesHelper.getCreateAccountStatus() == CreateAccountStatus.WAITING_FOR_SMS) {
			Lg.i(LOGTAG, "CreateAccountStatus = WAITING FOR SMS");
			startActivityForResult(new Intent(this, CreateAccountActivity.class), RESULT_CREATE_ACCOUNT_ACTIVITY);
		}
	}

	@Override
	protected void onPause()
	{
		Lg.i(LOGTAG, "onPause");
		super.onPause();
	}

	void updateButtonAccept()
	{
		final boolean enabled = !Util.isNullOrEmpty(mEditNumber.getText().toString());
		Lg.i(LOGTAG, "updateButtonAccept enabled=", enabled);
		mButtonAccept.setEnabled(enabled);
	}

	@SuppressWarnings("unused")
	public void createAccount(final View view)
	{
		final Integer countryCallingCode = (Integer) mSpinner.getSelectedItem();
		if (countryCallingCode == null) {
			Lg.e(LOGTAG, "createAccount no country code => aborting");
			return;
		}
		SimlarNumber.setDefaultRegion(countryCallingCode.intValue());

		final String number = mEditNumber.getText().toString();
		if (Util.isNullOrEmpty(number)) {
			Lg.e(LOGTAG, "createAccount no number => aborting");
			return;
		}

		// check telephoneNumbers plausibility
		final SimlarNumber simlarNumber = new SimlarNumber(number);
		if (!simlarNumber.isValid()) {
			(new AlertDialog.Builder(this))
					.setMessage(R.string.verify_number_activity_alert_wrong_number_text)
					.create().show();
			return;
		}

		final Intent intent = new Intent(this, CreateAccountActivity.class);
		intent.putExtra(CreateAccountActivity.INTENT_EXTRA_NUMBER, simlarNumber.getTelephoneNumber());
		startActivityForResult(intent, RESULT_CREATE_ACCOUNT_ACTIVITY);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		Lg.i(LOGTAG, "onActivityResult requestCode=", requestCode, " resultCode=", resultCode);
		if (requestCode == RESULT_CREATE_ACCOUNT_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				Lg.i(LOGTAG, "finishing on CreateAccount request");
				finish();
				startActivity(new Intent(this, MainActivity.class));
			}
		}
	}
}
