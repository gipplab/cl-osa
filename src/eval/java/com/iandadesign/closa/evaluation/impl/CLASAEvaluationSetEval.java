package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

class CLASAEvaluationSetEval {

    @Test
    void evalCLASAPan11Documents() {
        /*
            Ranks 1 to 50

            Precision: [64.4, 34.5, 23.800001, 14.96, 8.02, 4.3, 1.8479999]
            Recall: [64.4, 69.0, 71.4, 74.8, 80.2, 86.0, 92.4]
            F-Measure: [64.4, 46.0, 35.7, 24.933334, 14.581819, 8.190477, 3.6235292]

            Mean reciprocal rank: 69.69919188569743


            Aligned document similarities

            {520.0=3, 8330.0=1, 8420.0=1, 8430.0=1, 530.0=1, 8520.0=1, 8630.0=1, 8580.0=1, 540.0=1, 8710.0=1, 8720.0=1, 550.0=2, 8830.0=1, 8850.0=1, 560.0=1, 9150.0=1, 9210.0=1, 9240.0=2, 580.0=1, 9370.0=1, 9350.0=1, 9530.0=1, 9540.0=1, 610.0=1, 9740.0=1, 9860.0=1, 9950.0=1, 9980.0=1, 10040.0=1, 10050.0=1, 630.0=1, 10190.0=1, 640.0=1, 10250.0=1, 40.0=4, 10350.0=1, 650.0=2, 10480.0=1, 10620.0=1, 660.0=1, 10760.0=1, 10850.0=1, 10920.0=1, 10980.0=1, 11200.0=1, 11240.0=1, 11550.0=1, 11520.0=1, 11700.0=1, 730.0=1, 11780.0=2, 11830.0=1, 11880.0=1, 740.0=2, 11990.0=1, 12110.0=1, 12190.0=1, 760.0=1, 12260.0=1, 12410.0=1, 12470.0=1, 12420.0=1, 780.0=1, 12510.0=1, 12570.0=1, 12590.0=1, 12640.0=2, 790.0=1, 800.0=1, 12800.0=1, 12970.0=1, 12990.0=1, 13020.0=1, 13170.0=1, 13230.0=1, 13310.0=1, 13250.0=1, 13350.0=1, 13380.0=1, 840.0=1, 13540.0=1, 13790.0=1, 13800.0=2, 13810.0=1, 860.0=1, 13900.0=1, 14040.0=1, 14020.0=1, 880.0=2, 14080.0=1, 14120.0=1, 14150.0=1, 14350.0=1, 14500.0=1, 14530.0=3, 14640.0=1, 14610.0=1, 14690.0=1, 14780.0=1, 14910.0=1, 14850.0=1, 15010.0=2, 15100.0=1, 950.0=1, 60.0=4, 970.0=1, 15670.0=1, 15690.0=1, 980.0=1, 15750.0=1, 15940.0=1, 16010.0=1, 16060.0=2, 1010.0=1, 16160.0=1, 1020.0=1, 16450.0=1, 16630.0=1, 16700.0=1, 16650.0=1, 16680.0=1, 16750.0=1, 1060.0=1, 17130.0=1, 1070.0=2, 17140.0=1, 17190.0=1, 17350.0=1, 17320.0=1, 17280.0=1, 1080.0=1, 17420.0=1, 17540.0=1, 1100.0=1, 17680.0=1, 17730.0=1, 17700.0=1, 17950.0=1, 18140.0=1, 18200.0=2, 18410.0=1, 18420.0=1, 18530.0=1, 18500.0=1, 18640.0=1, 1160.0=2, 18810.0=1, 1170.0=1, 18840.0=1, 18830.0=1, 1190.0=1, 19140.0=2, 1200.0=1, 19290.0=1, 19420.0=1, 19340.0=1, 1210.0=1, 19560.0=1, 19460.0=1, 1220.0=1, 19520.0=1, 1230.0=2, 19820.0=1, 1250.0=1, 20200.0=1, 20230.0=1, 1270.0=1, 20450.0=1, 20460.0=1, 20600.0=1, 80.0=3, 20660.0=1, 20870.0=1, 1310.0=1, 21120.0=1, 21160.0=1, 21480.0=1, 1340.0=1, 1350.0=2, 21520.0=1, 21540.0=1, 21700.0=1, 21920.0=1, 1370.0=2, 21940.0=1, 1390.0=1, 22340.0=1, 1400.0=1, 1410.0=1, 1420.0=2, 22880.0=1, 23010.0=1, 90.0=5, 1450.0=2, 23230.0=1, 23460.0=1, 23500.0=1, 23580.0=1, 1490.0=3, 23880.0=1, 1510.0=3, 24400.0=1, 1530.0=1, 24490.0=1, 1540.0=1, 1570.0=2, 100.0=4, 1610.0=1, 26340.0=1, 1640.0=3, 1650.0=1, 26590.0=1, 1680.0=1, 1740.0=2, 1750.0=1, 1760.0=2, 110.0=2, 1780.0=1, 1810.0=2, 1820.0=1, 1840.0=3, 1850.0=2, 1880.0=2, 1900.0=1, 120.0=5, 1920.0=1, 1930.0=2, 1940.0=1, 1950.0=1, 1990.0=1, 2000.0=1, 2010.0=1, 2040.0=1, 2050.0=2, 2080.0=1, 2090.0=1, 130.0=1, 2130.0=1, 2150.0=3, 2170.0=1, 2180.0=2, 2190.0=1, 2210.0=2, 2220.0=1, 2230.0=1, 140.0=3, 2260.0=1, 2300.0=1, 2380.0=1, 150.0=4, 2440.0=1, 2460.0=1, 2450.0=1, 2470.0=1, 2480.0=1, 2500.0=1, 2530.0=2, 2550.0=1, 160.0=5, 10.0=2, 2590.0=1, 2580.0=2, 2620.0=2, 2610.0=1, 2650.0=1, 2670.0=1, 2700.0=2, 2730.0=2, 170.0=1, 2740.0=1, 2780.0=1, 2800.0=1, 2820.0=1, 180.0=5, 2890.0=1, 2920.0=1, 2940.0=2, 2930.0=1, 2970.0=1, 3030.0=1, 3040.0=1, 190.0=1, 3080.0=1, 3120.0=1, 3140.0=1, 3180.0=1, 3190.0=1, 200.0=6, 3200.0=1, 3260.0=1, 3250.0=1, 3360.0=1, 210.0=3, 3400.0=1, 3410.0=1, 220.0=3, 3580.0=2, 3650.0=1, 3660.0=1, 230.0=2, 3690.0=1, 3720.0=1, 3820.0=1, 3810.0=1, 240.0=4, 3910.0=1, 3960.0=1, 3980.0=2, 3970.0=1, 250.0=1, 4030.0=1, 4080.0=1, 260.0=3, 4210.0=1, 4220.0=1, 4230.0=1, 4240.0=1, 4290.0=1, 270.0=2, 4500.0=1, 280.0=2, 4510.0=1, 4610.0=1, 290.0=2, 300.0=5, 4980.0=1, 5100.0=1, 5150.0=1, 320.0=3, 20.0=2, 330.0=6, 5350.0=1, 340.0=3, 5450.0=1, 5550.0=1, 5540.0=1, 350.0=3, 5610.0=1, 5760.0=2, 360.0=1, 370.0=3, 5940.0=1, 380.0=2, 410.0=1, 6610.0=2, 420.0=2, 430.0=1, 440.0=1, 7130.0=2, 7170.0=1, 450.0=3, 7280.0=1, 7470.0=1, 470.0=2, 7540.0=1, 7580.0=1, 7660.0=1, 480.0=3, 30.0=6, 7680.0=1, 7860.0=1, 7900.0=1, 500.0=1, 8010.0=1}

         */
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es",
                    500
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAPan11Sentences() {
        /*
            Ranks 1 to 50

            Precision: [35.0, 18.9, 13.2, 8.48, 4.56, 2.51, 1.196]
            Recall: [35.0, 37.8, 39.6, 42.399998, 45.6, 50.199997, 59.8]
            F-Measure: [35.0, 25.199997, 19.8, 14.133334, 8.290909, 4.7809525, 2.345098]

            Mean reciprocal rank: 39.04097183238152


            Aligned document similarities

            {0.0=2, 520.0=1, 540.0=1, 550.0=1, 560.0=3, 570.0=4, 580.0=2, 590.0=1, 600.0=5, 620.0=1, 630.0=1, 640.0=2, 650.0=2, 670.0=1, 680.0=1, 690.0=1, 700.0=1, 710.0=5, 760.0=1, 770.0=1, 780.0=2, 810.0=2, 820.0=1, 860.0=2, 870.0=1, 880.0=1, 900.0=1, 910.0=2, 960.0=1, 970.0=1, 980.0=1, 990.0=1, 1000.0=2, 1030.0=1, 1040.0=2, 1060.0=1, 1080.0=3, 1100.0=1, 1110.0=1, 1120.0=1, 1170.0=1, 1210.0=2, 1230.0=2, 1240.0=1, 1290.0=1, 1320.0=2, 1330.0=1, 1360.0=2, 1380.0=2, 1400.0=1, 1450.0=2, 1460.0=1, 1490.0=1, 1550.0=1, 1560.0=3, 1570.0=1, 1580.0=1, 1590.0=1, 1650.0=1, 1730.0=1, 1750.0=1, 1770.0=1, 1800.0=1, 1810.0=2, 1840.0=1, 1880.0=1, 1910.0=1, 1920.0=1, 1930.0=1, 1970.0=1, 2020.0=1, 2030.0=1, 2040.0=2, 32890.0=1, 2080.0=1, 2090.0=1, 33550.0=1, 2120.0=1, 2140.0=1, 34490.0=1, 34950.0=1, 35790.0=1, 2250.0=1, 2260.0=1, 2270.0=1, 36150.0=1, 36750.0=1, 2410.0=1, 38450.0=1, 38930.0=1, 2440.0=1, 2460.0=1, 39490.0=1, 2510.0=2, 40350.0=1, 2540.0=1, 2550.0=1, 40790.0=1, 41450.0=1, 2600.0=1, 42350.0=1, 2640.0=1, 2680.0=1, 43190.0=1, 2760.0=1, 45110.0=1, 2850.0=1, 2900.0=2, 2960.0=2, 2970.0=1, 3000.0=1, 3030.0=1, 3140.0=1, 3200.0=1, 3210.0=1, 3220.0=1, 3260.0=1, 3280.0=1, 3310.0=1, 3340.0=1, 3350.0=1, 3360.0=1, 3370.0=1, 3450.0=2, 3490.0=1, 3510.0=1, 3570.0=1, 3590.0=1, 3630.0=1, 3640.0=2, 3670.0=1, 3730.0=1, 3780.0=1, 3800.0=1, 3860.0=1, 3950.0=1, 3980.0=2, 4030.0=1, 4020.0=1, 4080.0=1, 4210.0=1, 4230.0=1, 4270.0=1, 4300.0=2, 4320.0=1, 4330.0=1, 4390.0=1, 4560.0=1, 4590.0=1, 4620.0=1, 4640.0=1, 4660.0=2, 4810.0=1, 4900.0=1, 4910.0=1, 4930.0=1, 4980.0=1, 5030.0=1, 5140.0=1, 5170.0=1, 5210.0=1, 5220.0=1, 5280.0=1, 5310.0=1, 5300.0=1, 5390.0=1, 5530.0=1, 5550.0=1, 5540.0=1, 5590.0=1, 5630.0=1, 5730.0=1, 5760.0=1, 5910.0=2, 5990.0=1, 6010.0=1, 6020.0=1, 6040.0=1, 6110.0=1, 6490.0=1, 6530.0=1, 6580.0=1, 6750.0=1, 6770.0=2, 6810.0=1, 6840.0=1, 6910.0=1, 6900.0=1, 6920.0=1, 7020.0=1, 7320.0=1, 7330.0=1, 7410.0=1, 7450.0=1, 7690.0=1, 30.0=1, 7770.0=1, 7840.0=1, 7870.0=1, 8510.0=1, 8520.0=1, 8630.0=1, 8870.0=1, 9550.0=1, 9760.0=1, 9820.0=1, 9980.0=1, 40.0=1, 10320.0=1, 10360.0=1, 11200.0=1, 11270.0=1, 12030.0=1, 12040.0=1, 12290.0=1, 12710.0=1, 12760.0=1, 50.0=1, 12990.0=1, 13200.0=1, 13220.0=1, 13510.0=1, 14160.0=1, 15210.0=1, 15270.0=1, 15340.0=1, 15310.0=1, 60.0=2, 15640.0=1, 15920.0=1, 16180.0=1, 16450.0=1, 16620.0=1, 16610.0=1, 16820.0=1, 17200.0=1, 17490.0=1, 17470.0=1, 17980.0=1, 70.0=2, 19130.0=1, 19510.0=1, 19640.0=1, 19930.0=1, 19850.0=1, 19960.0=1, 20570.0=1, 80.0=3, 20870.0=1, 21390.0=1, 21400.0=1, 21730.0=1, 22170.0=1, 22610.0=1, 23080.0=1, 23260.0=1, 23410.0=1, 23350.0=1, 23340.0=1, 23780.0=1, 23920.0=1, 23840.0=1, 24040.0=1, 24350.0=1, 24360.0=1, 24500.0=1, 24570.0=1, 24680.0=1, 24720.0=1, 24910.0=1, 25180.0=1, 25150.0=1, 25190.0=1, 25440.0=1, 25500.0=1, 25650.0=1, 100.0=1, 25760.0=1, 25960.0=1, 25990.0=1, 26020.0=1, 26210.0=1, 26320.0=1, 26430.0=3, 26670.0=1, 26630.0=1, 26690.0=1, 26950.0=1, 27230.0=1, 27190.0=1, 27300.0=1, 27340.0=1, 27430.0=2, 27460.0=2, 27490.0=1, 27720.0=1, 27730.0=1, 27870.0=1, 27980.0=1, 28220.0=1, 110.0=2, 28540.0=1, 28580.0=2, 28560.0=1, 28650.0=1, 29330.0=1, 29680.0=1, 29820.0=1, 29830.0=1, 30140.0=1, 30280.0=1, 30270.0=1, 30300.0=1, 30260.0=1, 30420.0=1, 30520.0=1, 30690.0=1, 30730.0=1, 120.0=2, 30940.0=1, 30880.0=1, 30900.0=1, 31000.0=1, 31230.0=1, 31270.0=1, 31400.0=1, 31470.0=1, 31520.0=1, 31620.0=1, 31690.0=1, 31740.0=1, 32080.0=1, 32220.0=1, 32350.0=1, 32880.0=1, 32940.0=1, 33500.0=1, 130.0=2, 33420.0=1, 33860.0=1, 34420.0=1, 34480.0=1, 140.0=3, 36100.0=2, 36220.0=1, 38000.0=1, 150.0=2, 39140.0=1, 39000.0=1, 39060.0=1, 40000.0=1, 40340.0=1, 40680.0=1, 160.0=2, 41580.0=1, 170.0=4, 180.0=2, 190.0=2, 200.0=2, 210.0=4, 220.0=2, 230.0=3, 240.0=2, 250.0=5, 260.0=1, 270.0=3, 280.0=1, 290.0=1, 300.0=1, 310.0=5, 320.0=1, 330.0=3, 350.0=3, 360.0=3, 370.0=3, 380.0=1, 390.0=1, 400.0=2, 420.0=1, 430.0=3, 440.0=1, 450.0=6, 460.0=3, 470.0=5, 480.0=3, 490.0=2, 500.0=3, 510.0=1}


         */
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es",
                    500
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAJrcAcquis() {
        /*

            Ranks 1 to 50

            Precision: [56.199997, 30.4, 21.533333, 13.719999, 7.48, 4.2599998, 1.916]
            Recall: [56.199997, 60.8, 64.600006, 68.6, 74.8, 85.2, 95.8]
            F-Measure: [56.199997, 40.533333, 32.3, 22.866667, 13.6, 8.114285, 3.7568629]

            Mean reciprocal rank: 62.67319319666734


            Aligned document similarities

            {0.0=4, 650.0=1, 800.0=1, 970.0=1, 990.0=1, 1020.0=1, 1030.0=1, 1060.0=1, 1110.0=2, 71190.0=1, 1420.0=1, 1460.0=1, 1470.0=1, 1600.0=1, 104030.0=1, 1710.0=1, 1930.0=1, 1960.0=1, 2030.0=1, 33390.0=1, 33610.0=1,
            2110.0=1, 2100.0=1, 34310.0=1, 34570.0=1, 34690.0=1, 2260.0=1, 36190.0=1, 2270.0=1, 2280.0=1, 2310.0=2, 2340.0=1, 2410.0=1, 38550.0=1, 38590.0=1, 2420.0=1, 2450.0=1, 39330.0=1, 2480.0=1, 41870.0=1, 2650.0=1, 2
            660.0=1, 42970.0=1, 2710.0=1, 2720.0=1, 43990.0=1, 2750.0=2, 2760.0=1, 2780.0=1, 2860.0=1, 2890.0=1, 2900.0=2, 47110.0=1, 2960.0=2, 2990.0=2, 2980.0=1, 48950.0=1, 3100.0=1, 3180.0=2, 3170.0=1, 3240.0=1, 3270.0=1, 53410.0=1, 3410.0=2, 3450.0=1, 3510.0=1, 3520.0=1, 3530.0=1, 3550.0=1, 3610.0=2, 3650.0=1, 3670.0=1, 3710.0=2, 3720.0=1, 3760.0=1, 3780.0=1, 3800.0=2, 3820.0=1, 3860.0=1, 3870.0=1, 3930.0=1, 3920.0=1, 4090.0=1, 4100.0=1, 4180.0=1, 4220.0=1, 4270.0=1, 4340.0=1, 4350.0=1, 4360.0=2, 4440.0=1, 4460.0=1, 4510.0=1, 4480.0=1, 4540.0=1, 4550.0=1, 4580.0=1, 4640.0=1, 4660.0=1, 4690.0=2, 4720.0=1, 4790.0=1, 4770.0=1, 4800.0=1, 4830.0=1, 4860.0=2, 4840.0=1, 4890.0=1, 4870.0=1, 4910.0=1, 4920.0=1, 4990.0=1, 4960.0=1, 5010.0=2, 5130.0=2, 5160.0=1, 5180.0=1, 5220.0=1, 5260.0=1, 5340.0=1, 5320.0=1, 5370.0=1, 86940.0=1, 5460.0=1, 5480.0=1, 5530.0=1, 5550.0=1, 5660.0=1, 5670.0=3, 5720.0=2, 5700.0=1, 5820.0=1, 5840.0=1, 5880.0=1, 5930.0=1, 5920.0=1, 6060.0=2, 6050.0=1, 6120.0=1, 6160.0=2, 6150.0=1, 6250.0=2, 6300.0=1, 6330.0=1, 6380.0=1, 6420.0=1, 6410.0=1, 6430.0=1, 6440.0=1, 6460.0=1, 6490.0=2, 6500.0=1, 6590.0=1, 6600.0=1, 6640.0=2, 6680.0=1, 6660.0=2, 6720.0=1, 6730.0=1, 6770.0=1, 6800.0=1, 6820.0=5, 6830.0=1, 6880.0=1, 6910.0=1, 6930.0=1, 6950.0=1, 6980.0=1, 7020.0=1, 7080.0=1, 7150.0=1, 7190.0=2, 7170.0=1, 7200.0=1, 7240.0=2, 7260.0=1, 7270.0=1, 7300.0=1, 7350.0=1, 7390.0=1, 7410.0=2, 7440.0=1, 7450.0=1, 7430.0=1, 7550.0=1, 7560.0=2, 7590.0=1, 7690.0=1, 7730.0=2, 7750.0=2, 7800.0=1, 7790.0=1, 7820.0=1, 7890.0=1, 7880.0=1, 7910.0=1, 7920.0=1, 7940.0=2, 7980.0=1, 7970.0=1, 8070.0=1, 8140.0=1, 8210.0=1, 8220.0=1, 8200.0=1, 8260.0=1, 8350.0=1, 8320.0=1, 8360.0=2, 8340.0=1, 8440.0=1, 8390.0=1, 8510.0=1, 8450.0=1, 8480.0=1, 8490.0=1, 8550.0=1, 8610.0=2, 8690.0=1, 8680.0=1, 8760.0=1, 8820.0=1, 8770.0=3, 8800.0=1, 8780.0=1, 8860.0=1, 8950.0=1, 8930.0=1, 8900.0=1, 8970.0=1, 9010.0=2, 9000.0=1, 9050.0=1, 9030.0=2, 9060.0=1, 9090.0=1, 9190.0=1, 9240.0=1, 9220.0=1, 9290.0=1, 9320.0=2, 9340.0=1, 9300.0=1, 9310.0=1, 9380.0=1, 9510.0=2, 9580.0=1, 9540.0=1, 9680.0=1, 9720.0=1, 9690.0=1, 9700.0=1, 9790.0=1, 9850.0=1, 9840.0=1, 9900.0=1, 9890.0=1, 9970.0=1, 9930.0=1, 10030.0=1, 10060.0=1, 10080.0=1, 10070.0=1, 10200.0=1, 10220.0=1, 10270.0=1, 10340.0=2, 10330.0=1, 10370.0=1, 10450.0=1, 10480.0=1, 10510.0=1, 10610.0=1, 10670.0=2, 10630.0=1, 10740.0=2, 10780.0=2, 10770.0=1, 10810.0=1, 10820.0=1, 10900.0=2, 10940.0=1, 11030.0=1, 11010.0=1, 11060.0=1, 11040.0=1, 11290.0=2, 11320.0=1, 11430.0=1, 11400.0=2, 11630.0=1, 11730.0=1, 11900.0=1, 11840.0=1, 11990.0=1, 12040.0=1, 12090.0=1, 12150.0=1, 12120.0=1, 12210.0=2, 12170.0=1, 12280.0=1, 12350.0=1, 12310.0=1, 12430.0=1, 12440.0=1, 12510.0=1, 12480.0=1, 12530.0=1, 12590.0=1, 12680.0=1, 12840.0=1, 12940.0=1, 13040.0=1, 13000.0=1, 13110.0=1, 13060.0=1, 13240.0=2, 13690.0=1, 13680.0=1, 13790.0=1, 13800.0=1, 13850.0=1, 13910.0=1, 14030.0=2, 14190.0=1, 14180.0=1, 14270.0=1, 14370.0=1, 14460.0=1, 14420.0=1, 14470.0=1, 14490.0=1, 14500.0=1, 14700.0=1, 14740.0=2, 14900.0=1, 15050.0=1, 15040.0=1, 15100.0=1, 15300.0=1, 15330.0=1, 15380.0=1, 15480.0=1, 15650.0=1, 15620.0=1, 15800.0=1, 15750.0=1, 15860.0=1, 15930.0=1, 16190.0=1, 16150.0=1, 16230.0=1, 16280.0=1, 16460.0=1, 17070.0=1, 17090.0=1, 68970.0=1, 17620.0=1, 17570.0=1, 17800.0=1, 17890.0=1, 18010.0=1, 18020.0=1, 18120.0=1, 18290.0=1, 18280.0=1, 18220.0=1, 18350.0=1, 18450.0=1, 18650.0=1, 18710.0=1, 18800.0=1, 19050.0=1, 18990.0=1, 19080.0=1, 19150.0=1, 19100.0=1, 19250.0=1, 19550.0=1, 19570.0=1, 19670.0=1, 19600.0=1, 20160.0=1, 20230.0=1, 20380.0=1, 20450.0=1, 20520.0=1, 20840.0=1, 20900.0=1, 21000.0=1, 21090.0=1, 21280.0=1, 21730.0=1, 22170.0=1, 22360.0=1, 22440.0=1, 22530.0=1, 22650.0=1, 22680.0=1, 22710.0=1, 23020.0=1, 23150.0=1, 23210.0=1, 23180.0=1, 23630.0=1, 23580.0=1, 23650.0=1, 24670.0=1, 24970.0=1, 25040.0=1, 25350.0=1, 25410.0=1, 26790.0=1, 107930.0=1, 27380.0=1, 27940.0=1, 28080.0=1, 28210.0=1, 28520.0=1, 28750.0=1, 28940.0=1, 29500.0=1, 29570.0=1, 29850.0=1, 30010.0=1, 30220.0=1, 31060.0=1, 33180.0=1, 33040.0=1, 33580.0=1, 41760.0=1, 45120.0=1, 49800.0=1, 51380.0=1, 51880.0=1, 55300.0=1, 55400.0=1, 74720.0=1, 98520.0=1, 430.0=1, 124280.0=1}
        */
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparl() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
