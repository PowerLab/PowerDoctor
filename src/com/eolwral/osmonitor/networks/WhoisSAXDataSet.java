package com.eolwral.osmonitor.networks;

public class WhoisSAXDataSet 
{
    private String ip = "NA";
    private String country = "NA";
    private String region = "NA";
    private String isp = "NA";
    private String org = "NA";
    private String latitude = "NA";
    private String longitude = "NA";

	public void setip(String newip)
	{
		
		ip = newip;
	}

	public String getip()
	{
		return ip;
	}

	public void setcountry(String newcountry)
	{
		country = newcountry;
	}

	public String getcountry()
	{
		return country;
	}

	public void setregion(String newregion)
	{
		region = newregion;
	}

	public String getregion()
	{
		return region;
	}

	public void setisp(String newisp)
	{
		isp = newisp;
	}

	public String getisp()
	{
		return isp;
	}

	public void setorg(String neworg)
	{
		org = neworg;
	}

	public String getorg()
	{
		return org;
	}

	public void setlatitude(String newlatitude)
	{
		latitude = newlatitude;
	}

	public String getlatitude()
	{
		return latitude;
	}

	public void setlongitude(String newlongitude)
	{
		longitude = newlongitude;
	}

	public String getlongitude()
	{
		return longitude;
	}
	
	public String toString()
	{
		StringBuilder whoisInfo = new StringBuilder();
		whoisInfo.append("IP: "+ip);
		whoisInfo.append("\nCountry: "+country);
		whoisInfo.append("\nRegion: "+region);
		whoisInfo.append("\nISP: "+isp);
		whoisInfo.append("\nOrg: "+org);
		whoisInfo.append("\nLatitude: "+latitude);
		whoisInfo.append("\nLongitude: "+longitude);
		return whoisInfo.toString(); 
	}
	
	public double getMapnLatitude()
	{
		return Double.parseDouble(latitude)*1E6;
	}
	
	public double getMapLongtiude()
	{
		return Double.parseDouble(longitude)*1E6;
	}

}