package de.Ste3et_C0st.FurnitureLib.Database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.Utilitis.CallbackObjectIDs;
import de.Ste3et_C0st.FurnitureLib.main.ChunkData;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureManager;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.DataBaseType;
import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Objects;

public abstract class Database {
    public FurnitureLib plugin;
    private HikariConfig config;
    private HikariDataSource dataSource;
    private Converter converter;

    public Database(FurnitureLib instance, HikariConfig config) {
        this.plugin = instance;
        this.config = config;
        this.dataSource = new HikariDataSource(config);
        this.converter = new Converter(this);
    }

    public abstract DataBaseType getType();

    public HikariConfig getConfig() {
        return this.config;
    }

    public Connection getConnection() {
        try {
            Connection connection = this.dataSource.getConnection();
            if (connection == null) {
                throw new SQLException("Unable to get a connection from the pool.");
            }
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(ObjectID id) {
        String binary = FurnitureLib.getInstance().getSerializer().SerializeObjectID(id);
        int x = id.getStartLocation().getBlockX() >> 4;
        int z = id.getStartLocation().getBlockZ() >> 4;
        String sql = "REPLACE INTO furnitureLibData (ObjID, Data, world, `x`, `z`, `uuid`) " +
                "VALUES (" +
                "'" + id.getID() + "'," +
                "'" + binary + "'," +
                "'" + id.getWorldName() + "'," +
                +x + "," +
                +z + "," +
                "'" + id.getUUID().toString() + "');";
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void loadAsynchron(ChunkData chunkdata, CallbackObjectIDs callBack) {
        Bukkit.getScheduler().runTaskAsynchronously(FurnitureLib.getInstance(), () -> {
            String query = "SELECT ObjID,Data,world FROM furnitureLibData WHERE x=" + chunkdata.getX() + " AND z=" + chunkdata.getZ() + " AND world='" + chunkdata.getWorld() + "'";
            try (Connection con = getConnection(); ResultSet rs = con.createStatement().executeQuery(query)) {
                HashSet<ObjectID> idList = new HashSet<ObjectID>();
                if (rs.next()) {
                    do {
                        String a = rs.getString(1), c = rs.getString(2), d = rs.getString(3);
                        if (Objects.nonNull(a) && Objects.nonNull(c)) {
                            ObjectID obj = FurnitureLib.getInstance().getDeSerializer().Deserialize(a, c, SQLAction.NOTHING, d);
                            if (Objects.nonNull(obj)) {
                                idList.add(obj);
                            }
                        }
                    } while (rs.next());
                    FurnitureLib.debug("FurnitureLib load " + idList.size() + " Models for chunk " + " x:" + chunkdata.getX() + " z:" + chunkdata.getZ());
                }
                callBack.onResult(idList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadAll(SQLAction action) {
        long time1 = System.currentTimeMillis();
        //FurnitureLib.getInstance().getProjectManager().loadProjectFiles();
        try (Connection con = getConnection(); ResultSet rs = con.createStatement().executeQuery("SELECT ObjID,Data,world FROM furnitureLibData")) {
            HashSet<ObjectID> idList = new HashSet<ObjectID>();
            if (rs.next() == true) {
                long time2 = System.currentTimeMillis();
                SimpleDateFormat time = new SimpleDateFormat("mm:ss.SSS");
                String timeStr = time.format(time2 - time1);
                System.out.println("FurnitureLib load data from Source Finish Start deserialize [" + timeStr + "]");
                do {
                    String a = rs.getString(1), c = rs.getString(2), d = rs.getString(3);
                    if (!(a.isEmpty() || c.isEmpty())) {
                        ObjectID obj = FurnitureLib.getInstance().getDeSerializer().Deserialize(a, c, action, d);
                        if (Objects.nonNull(obj)) {
                            idList.add(obj);
                        }
                    }
				} while (rs.next());
            }
            FurnitureManager.getInstance().addObjectID(idList);
            plugin.getLogger().info("FurnitureLib load " + idList.size() + " Objects from: " + getType().name() + " Database");
            long time2 = System.currentTimeMillis();
            SimpleDateFormat time = new SimpleDateFormat("mm:ss.SSS");
            String timeStr = time.format(time2 - time1);
            int ArmorStands = FurnitureLib.getInstance().getDeSerializer().armorStands.get();
            int purged = FurnitureLib.getInstance().getDeSerializer().purged;
            plugin.getLogger().info("FurnitureLib has loaded " + ArmorStands + " in " + timeStr);
            plugin.getLogger().info("FurnitureLib has purged " + purged + " Objects");

            /* Load Blocks */
            idList.forEach(ObjectID::loadBlocks);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FurnitureManager.getInstance().getProjects().forEach(Project::applyFunction);
        }
    }

    public void delete(ObjectID objID) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("DELETE FROM furnitureLibData WHERE ObjID = '" + objID.getID() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Converter getConverter() {
        return this.converter;
    }
}