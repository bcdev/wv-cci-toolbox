Diese Software ist nicht als *produktiv* gedacht, 
es ist nur ein breaboard, um alle in- und outputs zu vergleichen, 
und um gegebenfalls zwischenergebnisse zum debuggen zu nutzen 


Wesentliche Änderungen vom letzten Mal:

    1. die AOT Dimension im  Ozean ist logarithmisch 
    2. Die Measurement-error-covarianzen für den Ozean und fürs Land
        werden jetzt pixelweise berechnet. Hintergrund ist, dass  die 
        Dimensionen in den LUTs für die Abs-kanäle ja auch 
        a) logaritmisch sind
        b) extra/intra-polierte Fensterkanäle und AMF enthalten

    3. Über dem Ozean nutze ich die vorberechneten LUTs für die Jakobians
        nicht mehr.  (Ob das wirklich wesentlich ist, bin ich mir nicht sicher)

Wesentliche Änderunegen diesmal:
    1. Neue LUTs
    2. Ich nutze die vorberechneten LUTs nicht mehr
   

Test Run:
    Benötigt:
        python 3.7 mit den standard libs + numba
        py-hdf: Das ist leider ein altes Stück 
                software. Man braucht sie um MODIS hdf4
                Daten mit python zu lesen. Bei mir ließ 
                es sich  mit pip installieren, aber erst, 
                nachdem ich explizit:
                $> export  INCLUDE_DIRS=/usr/include/hdf
                $> export  LIBRARY_DIRS=/usr/lib64/hdf
                (bash  linux) gesetzt habe. Die include 
                und lib -files gibt es noch in den 
                standard repos der diversen linuxe.
                Ich weiß nicht, wie es mit anaconda und windows 
                aussieht 
        netCDF4: Bei manchen Versionen von netCDF4 kann man hdf4  
                lesen ...Manchmal sind die entsprechenden 
                libs eingebunden, machmal nicht ...
        Je nach dem was geht, musst du modis_l1b oder modis_l1b_alt
        importieren.
        
    Aufruf:
        $>  cd bla/blo/consolidated_cci_luts/modis
        $>  python demo_modis_processor.py MOD21.....hdf Ergebniss.nc4
        also: 
        $>  python demo_modis_processor.py test_data/MOD021KM.A2010209.1050.061.2017251234617.hdf TCWV.A2010209.1050.nc4

Wichtig: Ich habe für dieses Beispiel die ERA Daten und die MODIS Wolkenmaske schon präpariert und in ein entsprechendes
         File(hier: ERA.A2010209.1050.nc4)  gepackt. Das ist ziemlich unflexibel, aber für ein breadboard sollte es reichen...
         
         
