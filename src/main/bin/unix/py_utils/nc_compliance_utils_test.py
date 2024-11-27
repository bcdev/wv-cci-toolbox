import unittest

import nc_compliance_utils as ncu
import numpy as np


class MyTestCase(unittest.TestCase):
    def test_something(self):
        self.assertEqual(True, False)  # add assertion here

    def test_get_sza_from_doy(self):
        lat = 0.
        doy = 355
        sza = ncu.get_sza_from_doy(doy, lat)
        self.assertAlmostEqual(23.45, sza, delta=0.01)

    def test_get_sza_from_date(self):
        lat = 0.
        yyyy = 2001
        mm = 12
        dd = 21
        sza = ncu.get_sza_from_date(yyyy, mm, dd, lat)
        self.assertAlmostEqual(23.45, sza, delta=0.01)

    def test_get_sza_array_from_date(self):
        lat = np.array([[0.0, 0.0, 0.0], [30.0, 30.0, 30.0]])
        yyyy = 2001
        mm = 12
        dd = 21

        sza = ncu.get_sza_from_date(yyyy, mm, dd, lat)
        self.assertAlmostEqual(23.45, sza[0][0], delta=0.01)
        self.assertAlmostEqual(53.45, sza[1][2], delta=0.01)
        sza_expected = np.array([[23.45, 23.45, 23.45], [53.45, 53.45, 53.45]])
        self.assertAlmostEqual(sza.all(), sza_expected.all(), delta=0.01)

        mm = 6
        sza = ncu.get_sza_from_date(yyyy, mm, dd, lat)
        self.assertAlmostEqual(23.45, sza[0][0], delta=0.01)
        self.assertAlmostEqual(6.55, sza[1][2], delta=0.01)
        sza_expected = np.array([[23.45, 23.45, 23.45], [6.55, 6.55, 6.55]])
        self.assertAlmostEqual(sza.all(), sza_expected.all(), delta=0.01)

        mm = 3
        dd = 21
        sza = ncu.get_sza_from_date(yyyy, mm, dd, lat)
        self.assertAlmostEqual(0.5, sza[0][0], delta=0.01)
        self.assertAlmostEqual(30.5, sza[1][2], delta=0.01)
        sza_expected = np.array([[0.5, 0.5, 0.5], [30.5, 30.5, 30.5]])
        self.assertAlmostEqual(sza.all(), sza_expected.all(), delta=0.01)

        mm = 9
        dd = 21
        sza = ncu.get_sza_from_date(yyyy, mm, dd, lat)
        self.assertAlmostEqual(0.1, sza[0][0], delta=0.01)
        self.assertAlmostEqual(30.1, sza[1][2], delta=0.01)
        sza_expected = np.array([[0.1, 0.1, 0.1], [30.1, 30.1, 30.1]])
        self.assertAlmostEqual(sza.all(), sza_expected.all(), delta=0.01)



if __name__ == '__main__':
    unittest.main()
