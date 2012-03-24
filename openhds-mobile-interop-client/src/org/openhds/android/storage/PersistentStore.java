package org.openhds.android.storage;

import org.openhds.android.model.FormSubmissionRecord;
import org.openhds.android.model.User;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Persistent store
 */
public class PersistentStore {
	private static final String DATABASE_NAME = "form_submission";
	private static final int DATABASE_VERSION = 1;

	private static final String KEY_ID = "_id";

	private static final String FORM_TABLE_NAME = "formsubmission";
	public static final String KEY_FORMOWNER_ID = "form_owner_id";
	public static final String KEY_FORM_TYPE = "form_type";
	public static final String KEY_FORM_INSTANCE = "form_instance";
	private static final String FORM_DB_CREATE = "CREATE TABLE "
			+ FORM_TABLE_NAME + " (" + KEY_ID + " INTEGER PRIMARY KEY, "
			+ KEY_FORMOWNER_ID + " TEXT, " + KEY_FORM_TYPE + " TEXT, "
			+ KEY_FORM_INSTANCE + " TEXT)";

	private static final String ERROR_TABLE_NAME = "formsubmission_msg";
	public static final String KEY_FORM_ID = "form_id";
	public static final String KEY_FORM_MSG = "message";
	private static final String MESSAGE_DB_CREATE = "CREATE TABLE "
			+ ERROR_TABLE_NAME + " (" + KEY_FORM_ID + " INTEGER, "
			+ KEY_FORM_MSG + " TEXT)";

	private static final String USER_TABLE_NAME = "openhds_user";
	public static final String KEY_USER_NAME = "username";
	public static final String KEY_USER_PASS = "password";
	private static final String USER_DB_CREATE = "CREATE TABLE "
			+ USER_TABLE_NAME + " (" + KEY_ID + " INTEGER PRIMARY KEY, "
			+ KEY_USER_NAME + " TEXT, " + KEY_USER_PASS + " TEXT)";

	private static class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(FORM_DB_CREATE);
			db.execSQL(MESSAGE_DB_CREATE);
			db.execSQL(USER_DB_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	private DbHelper dbHelper;

	public PersistentStore(Context context) {
		dbHelper = new DbHelper(context);
	}

	public void saveFormSubmission(FormSubmissionRecord fs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues cv = new ContentValues();
			cv.put(KEY_FORMOWNER_ID, fs.getFormOwnerId());
			cv.put(KEY_FORM_TYPE, fs.getFormType());
			cv.put(KEY_FORM_INSTANCE, fs.getFormInstance());
			long rowId = db.insert(FORM_TABLE_NAME, null, cv);

			for (String error : fs.getErrors()) {
				cv = new ContentValues();
				cv.put(KEY_FORM_ID, rowId);
				cv.put(KEY_FORM_MSG, error);
				db.insert(ERROR_TABLE_NAME, null, cv);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public User findUserByUsername(String username) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		User user = null;
		try {
			Cursor c = db.query(USER_TABLE_NAME, getUserTableColumns(),
					KEY_USER_NAME + " = ?", new String[] { username }, null,
					null, null);
			boolean found = c.moveToNext();
			if (!found) {
				c.close();
				boolean i = db.isOpen();
				return null;
			}

			user = new User();
			user.setId(c.getLong(c.getColumnIndex(KEY_ID)));
			user.setName(c.getString(c.getColumnIndex(KEY_USER_NAME)));
			user.setPassword(c.getString(c.getColumnIndex(KEY_USER_PASS)));
			c.close();
		} catch (Exception e) {
			Log.w("findUserByUsername", e.getMessage());
		}

		return user;
	}

	private String[] getUserTableColumns() {
		return new String[] { KEY_ID, KEY_USER_NAME, KEY_USER_PASS };
	}

	public long userCount() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		long rows = DatabaseUtils.queryNumEntries(db, USER_TABLE_NAME);

		return rows;
	}

	public long addUser(User u) {
		long id = -1;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues cv = new ContentValues();
			cv.put(KEY_USER_NAME, u.getName());
			cv.put(KEY_USER_PASS, u.getPassword());

			id = db.insert(USER_TABLE_NAME, null, cv);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		return id;
	}

	public Cursor getFormsForUsername(String user) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = null;
		cursor = db.query(FORM_TABLE_NAME, getFormColumns(), KEY_FORMOWNER_ID
				+ " = ?", new String[] { user }, null, null, null);
		return cursor;
	}

	private String[] getFormColumns() {
		return new String[] { KEY_ID, KEY_FORM_TYPE, KEY_FORMOWNER_ID };
	}
}