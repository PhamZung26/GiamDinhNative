package com.tc128.giamdinhnative.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile PhotoDao _photoDao;

  private volatile LookupDao _lookupDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(8) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `containerNumber` TEXT, `itemEorId` INTEGER, `pathLocal` TEXT, `pathServer` TEXT, `serverId` INTEGER, `status` TEXT NOT NULL, `isUploaded` INTEGER NOT NULL, `isResized` INTEGER NOT NULL, `isBackedUp` INTEGER NOT NULL, `isDeletedLocal` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastError` TEXT, `uploadAttempts` INTEGER NOT NULL, `isSeal` INTEGER NOT NULL, `sealNumber` TEXT, `isSealUploaded` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `components` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `nameVn` TEXT, `nameEn` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `damage_codes` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `repair_methods` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `item_repairs` (`id` INTEGER NOT NULL, `componentId` INTEGER NOT NULL, `repairMethodId` INTEGER NOT NULL, `location` TEXT, `length` INTEGER, `wide` INTEGER, `sts` TEXT, `qty` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fast_fills` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cham_diems` (`id` INTEGER NOT NULL, `nhom` TEXT, `chiTiet` TEXT, `dienGiai` TEXT NOT NULL, `diemSo` INTEGER NOT NULL, `dinhNghia` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `grades` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sizes` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `opts` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `clean_methods` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `depots` (`id` INTEGER NOT NULL, `codeName` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2badfe616cac58df49c0fb571e5cea3f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `photos`");
        db.execSQL("DROP TABLE IF EXISTS `components`");
        db.execSQL("DROP TABLE IF EXISTS `damage_codes`");
        db.execSQL("DROP TABLE IF EXISTS `repair_methods`");
        db.execSQL("DROP TABLE IF EXISTS `item_repairs`");
        db.execSQL("DROP TABLE IF EXISTS `fast_fills`");
        db.execSQL("DROP TABLE IF EXISTS `cham_diems`");
        db.execSQL("DROP TABLE IF EXISTS `grades`");
        db.execSQL("DROP TABLE IF EXISTS `sizes`");
        db.execSQL("DROP TABLE IF EXISTS `opts`");
        db.execSQL("DROP TABLE IF EXISTS `clean_methods`");
        db.execSQL("DROP TABLE IF EXISTS `depots`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPhotos = new HashMap<String, TableInfo.Column>(17);
        _columnsPhotos.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("containerNumber", new TableInfo.Column("containerNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("itemEorId", new TableInfo.Column("itemEorId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("pathLocal", new TableInfo.Column("pathLocal", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("pathServer", new TableInfo.Column("pathServer", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("serverId", new TableInfo.Column("serverId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isUploaded", new TableInfo.Column("isUploaded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isResized", new TableInfo.Column("isResized", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isBackedUp", new TableInfo.Column("isBackedUp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isDeletedLocal", new TableInfo.Column("isDeletedLocal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("lastError", new TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("uploadAttempts", new TableInfo.Column("uploadAttempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isSeal", new TableInfo.Column("isSeal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("sealNumber", new TableInfo.Column("sealNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotos.put("isSealUploaded", new TableInfo.Column("isSealUploaded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPhotos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPhotos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPhotos = new TableInfo("photos", _columnsPhotos, _foreignKeysPhotos, _indicesPhotos);
        final TableInfo _existingPhotos = TableInfo.read(db, "photos");
        if (!_infoPhotos.equals(_existingPhotos)) {
          return new RoomOpenHelper.ValidationResult(false, "photos(com.tc128.giamdinhnative.data.local.PhotoEntity).\n"
                  + " Expected:\n" + _infoPhotos + "\n"
                  + " Found:\n" + _existingPhotos);
        }
        final HashMap<String, TableInfo.Column> _columnsComponents = new HashMap<String, TableInfo.Column>(4);
        _columnsComponents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComponents.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComponents.put("nameVn", new TableInfo.Column("nameVn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComponents.put("nameEn", new TableInfo.Column("nameEn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysComponents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesComponents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoComponents = new TableInfo("components", _columnsComponents, _foreignKeysComponents, _indicesComponents);
        final TableInfo _existingComponents = TableInfo.read(db, "components");
        if (!_infoComponents.equals(_existingComponents)) {
          return new RoomOpenHelper.ValidationResult(false, "components(com.tc128.giamdinhnative.data.local.ComponentEntity).\n"
                  + " Expected:\n" + _infoComponents + "\n"
                  + " Found:\n" + _existingComponents);
        }
        final HashMap<String, TableInfo.Column> _columnsDamageCodes = new HashMap<String, TableInfo.Column>(3);
        _columnsDamageCodes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDamageCodes.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDamageCodes.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDamageCodes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDamageCodes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDamageCodes = new TableInfo("damage_codes", _columnsDamageCodes, _foreignKeysDamageCodes, _indicesDamageCodes);
        final TableInfo _existingDamageCodes = TableInfo.read(db, "damage_codes");
        if (!_infoDamageCodes.equals(_existingDamageCodes)) {
          return new RoomOpenHelper.ValidationResult(false, "damage_codes(com.tc128.giamdinhnative.data.local.DamageCodeEntity).\n"
                  + " Expected:\n" + _infoDamageCodes + "\n"
                  + " Found:\n" + _existingDamageCodes);
        }
        final HashMap<String, TableInfo.Column> _columnsRepairMethods = new HashMap<String, TableInfo.Column>(3);
        _columnsRepairMethods.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairMethods.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairMethods.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRepairMethods = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRepairMethods = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRepairMethods = new TableInfo("repair_methods", _columnsRepairMethods, _foreignKeysRepairMethods, _indicesRepairMethods);
        final TableInfo _existingRepairMethods = TableInfo.read(db, "repair_methods");
        if (!_infoRepairMethods.equals(_existingRepairMethods)) {
          return new RoomOpenHelper.ValidationResult(false, "repair_methods(com.tc128.giamdinhnative.data.local.RepairMethodEntity).\n"
                  + " Expected:\n" + _infoRepairMethods + "\n"
                  + " Found:\n" + _existingRepairMethods);
        }
        final HashMap<String, TableInfo.Column> _columnsItemRepairs = new HashMap<String, TableInfo.Column>(8);
        _columnsItemRepairs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("componentId", new TableInfo.Column("componentId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("repairMethodId", new TableInfo.Column("repairMethodId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("location", new TableInfo.Column("location", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("length", new TableInfo.Column("length", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("wide", new TableInfo.Column("wide", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("sts", new TableInfo.Column("sts", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItemRepairs.put("qty", new TableInfo.Column("qty", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysItemRepairs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesItemRepairs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoItemRepairs = new TableInfo("item_repairs", _columnsItemRepairs, _foreignKeysItemRepairs, _indicesItemRepairs);
        final TableInfo _existingItemRepairs = TableInfo.read(db, "item_repairs");
        if (!_infoItemRepairs.equals(_existingItemRepairs)) {
          return new RoomOpenHelper.ValidationResult(false, "item_repairs(com.tc128.giamdinhnative.data.local.ItemRepairEntity).\n"
                  + " Expected:\n" + _infoItemRepairs + "\n"
                  + " Found:\n" + _existingItemRepairs);
        }
        final HashMap<String, TableInfo.Column> _columnsFastFills = new HashMap<String, TableInfo.Column>(3);
        _columnsFastFills.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFastFills.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFastFills.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFastFills = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFastFills = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFastFills = new TableInfo("fast_fills", _columnsFastFills, _foreignKeysFastFills, _indicesFastFills);
        final TableInfo _existingFastFills = TableInfo.read(db, "fast_fills");
        if (!_infoFastFills.equals(_existingFastFills)) {
          return new RoomOpenHelper.ValidationResult(false, "fast_fills(com.tc128.giamdinhnative.data.local.FastFillEntity).\n"
                  + " Expected:\n" + _infoFastFills + "\n"
                  + " Found:\n" + _existingFastFills);
        }
        final HashMap<String, TableInfo.Column> _columnsChamDiems = new HashMap<String, TableInfo.Column>(6);
        _columnsChamDiems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChamDiems.put("nhom", new TableInfo.Column("nhom", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChamDiems.put("chiTiet", new TableInfo.Column("chiTiet", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChamDiems.put("dienGiai", new TableInfo.Column("dienGiai", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChamDiems.put("diemSo", new TableInfo.Column("diemSo", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChamDiems.put("dinhNghia", new TableInfo.Column("dinhNghia", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChamDiems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChamDiems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChamDiems = new TableInfo("cham_diems", _columnsChamDiems, _foreignKeysChamDiems, _indicesChamDiems);
        final TableInfo _existingChamDiems = TableInfo.read(db, "cham_diems");
        if (!_infoChamDiems.equals(_existingChamDiems)) {
          return new RoomOpenHelper.ValidationResult(false, "cham_diems(com.tc128.giamdinhnative.data.local.ChamDiemEntity).\n"
                  + " Expected:\n" + _infoChamDiems + "\n"
                  + " Found:\n" + _existingChamDiems);
        }
        final HashMap<String, TableInfo.Column> _columnsGrades = new HashMap<String, TableInfo.Column>(3);
        _columnsGrades.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGrades = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGrades = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGrades = new TableInfo("grades", _columnsGrades, _foreignKeysGrades, _indicesGrades);
        final TableInfo _existingGrades = TableInfo.read(db, "grades");
        if (!_infoGrades.equals(_existingGrades)) {
          return new RoomOpenHelper.ValidationResult(false, "grades(com.tc128.giamdinhnative.data.local.GradeEntity).\n"
                  + " Expected:\n" + _infoGrades + "\n"
                  + " Found:\n" + _existingGrades);
        }
        final HashMap<String, TableInfo.Column> _columnsSizes = new HashMap<String, TableInfo.Column>(3);
        _columnsSizes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSizes.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSizes.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSizes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSizes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSizes = new TableInfo("sizes", _columnsSizes, _foreignKeysSizes, _indicesSizes);
        final TableInfo _existingSizes = TableInfo.read(db, "sizes");
        if (!_infoSizes.equals(_existingSizes)) {
          return new RoomOpenHelper.ValidationResult(false, "sizes(com.tc128.giamdinhnative.data.local.SizeEntity).\n"
                  + " Expected:\n" + _infoSizes + "\n"
                  + " Found:\n" + _existingSizes);
        }
        final HashMap<String, TableInfo.Column> _columnsOpts = new HashMap<String, TableInfo.Column>(3);
        _columnsOpts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOpts.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOpts.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOpts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOpts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOpts = new TableInfo("opts", _columnsOpts, _foreignKeysOpts, _indicesOpts);
        final TableInfo _existingOpts = TableInfo.read(db, "opts");
        if (!_infoOpts.equals(_existingOpts)) {
          return new RoomOpenHelper.ValidationResult(false, "opts(com.tc128.giamdinhnative.data.local.OptEntity).\n"
                  + " Expected:\n" + _infoOpts + "\n"
                  + " Found:\n" + _existingOpts);
        }
        final HashMap<String, TableInfo.Column> _columnsCleanMethods = new HashMap<String, TableInfo.Column>(3);
        _columnsCleanMethods.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCleanMethods.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCleanMethods.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCleanMethods = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCleanMethods = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCleanMethods = new TableInfo("clean_methods", _columnsCleanMethods, _foreignKeysCleanMethods, _indicesCleanMethods);
        final TableInfo _existingCleanMethods = TableInfo.read(db, "clean_methods");
        if (!_infoCleanMethods.equals(_existingCleanMethods)) {
          return new RoomOpenHelper.ValidationResult(false, "clean_methods(com.tc128.giamdinhnative.data.local.CleanMethodEntity).\n"
                  + " Expected:\n" + _infoCleanMethods + "\n"
                  + " Found:\n" + _existingCleanMethods);
        }
        final HashMap<String, TableInfo.Column> _columnsDepots = new HashMap<String, TableInfo.Column>(3);
        _columnsDepots.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDepots.put("codeName", new TableInfo.Column("codeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDepots.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDepots = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDepots = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDepots = new TableInfo("depots", _columnsDepots, _foreignKeysDepots, _indicesDepots);
        final TableInfo _existingDepots = TableInfo.read(db, "depots");
        if (!_infoDepots.equals(_existingDepots)) {
          return new RoomOpenHelper.ValidationResult(false, "depots(com.tc128.giamdinhnative.data.local.DepotEntity).\n"
                  + " Expected:\n" + _infoDepots + "\n"
                  + " Found:\n" + _existingDepots);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2badfe616cac58df49c0fb571e5cea3f", "68591be4fec2b6666e024bd92f2d3046");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "photos","components","damage_codes","repair_methods","item_repairs","fast_fills","cham_diems","grades","sizes","opts","clean_methods","depots");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `photos`");
      _db.execSQL("DELETE FROM `components`");
      _db.execSQL("DELETE FROM `damage_codes`");
      _db.execSQL("DELETE FROM `repair_methods`");
      _db.execSQL("DELETE FROM `item_repairs`");
      _db.execSQL("DELETE FROM `fast_fills`");
      _db.execSQL("DELETE FROM `cham_diems`");
      _db.execSQL("DELETE FROM `grades`");
      _db.execSQL("DELETE FROM `sizes`");
      _db.execSQL("DELETE FROM `opts`");
      _db.execSQL("DELETE FROM `clean_methods`");
      _db.execSQL("DELETE FROM `depots`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PhotoDao.class, PhotoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LookupDao.class, LookupDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PhotoDao photoDao() {
    if (_photoDao != null) {
      return _photoDao;
    } else {
      synchronized(this) {
        if(_photoDao == null) {
          _photoDao = new PhotoDao_Impl(this);
        }
        return _photoDao;
      }
    }
  }

  @Override
  public LookupDao lookupDao() {
    if (_lookupDao != null) {
      return _lookupDao;
    } else {
      synchronized(this) {
        if(_lookupDao == null) {
          _lookupDao = new LookupDao_Impl(this);
        }
        return _lookupDao;
      }
    }
  }
}
