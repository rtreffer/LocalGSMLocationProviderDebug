package org.gfd.gsmlocation.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CellInfo implements Parcelable {
    public int MCC = -1;
    public int MNC = -1;
    public int CID = -1;
    public int LAC = -1;
    public int dbm = 0;
    public double lat = 0d;
    public double lng = 0d;
    public long measurement;
    public long seen = System.currentTimeMillis();

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !CellInfo.class.equals(o.getClass())) {
            return false;
        }

        CellInfo cellInfo = (CellInfo) o;

        if (CID != cellInfo.CID) return false;
        if (LAC != cellInfo.LAC) return false;
        if (MCC != cellInfo.MCC) return false;
        if (MNC != cellInfo.MNC) return false;
        if (Double.compare(cellInfo.lat, lat) != 0) return false;
        if (Double.compare(cellInfo.lng, lng) != 0) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        result = MCC;
        result = 31 * result + MNC;
        result = 31 * result + CID;
        result = 31 * result + LAC;
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public void sanitize() {
        if (MCC == Integer.MAX_VALUE) MCC = -1;
        if (MNC == Integer.MAX_VALUE) MNC = -1;
        if (CID == Integer.MAX_VALUE) CID = -1;
        if (LAC == Integer.MAX_VALUE) LAC = -1;
    }

    public boolean isInvalid() {
        return CID == -1 && LAC == -1;
    }

    public String toString() {
        return "CellInfo(" +
                "MCC=" + MCC +
                ", MNC=" + MNC +
                ", CID=" + CID +
                ", LAC=" + LAC +
                ", dbm=" + dbm +
                ", lng=" + lng +
                ", lat=" + lat +
                ')';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(MCC);
        dest.writeInt(MNC);
        dest.writeInt(CID);
        dest.writeInt(LAC);
        dest.writeInt(dbm);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeLong(measurement);
        dest.writeLong(seen);
    }

    public static Parcelable.Creator<CellInfo> CREATOR =
        new Parcelable.Creator<CellInfo>() {

            @Override
            public CellInfo createFromParcel(Parcel source) {
                CellInfo info = new CellInfo();
                info.MCC = source.readInt();
                info.MNC = source.readInt();
                info.CID = source.readInt();
                info.LAC = source.readInt();
                info.lat = source.readDouble();
                info.lng = source.readDouble();
                info.measurement = source.readLong();
                info.seen = source.readLong();
                return info;
            }

            @Override
            public CellInfo[] newArray(int size) {
                return new CellInfo[size];
            }

        };

}
