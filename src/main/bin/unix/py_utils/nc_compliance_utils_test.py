import unittest

import nc_compliance_utils as ncu


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


if __name__ == '__main__':
    unittest.main()
