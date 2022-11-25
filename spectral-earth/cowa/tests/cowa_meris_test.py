import unittest
import numpy as np
import xarray as xr


# noinspection PyUnresolvedReferences
class TestCowaMeris(unittest.TestCase):
    def setUp(self):
        pass

    def test_function_itp_np_j_b(self):

        exx = 3
        lut = np.array([5, 5, 5, 5, 3, 3, 6, 5, 5, 3])
        mul = np.array([[168750, 33750, 6750, 1350, 450, 150, 25, 5, 1]])

    def test_sice2_io_nparrays_to_olci_scene_ds(self):
        print('*** NPARRAYS to OLCI_SCENE_DS ***')

        # 2*3 test numpy arrays...
        width = 2
        height = 3
        # width = 4864
        # height = 4085

        sza_data = np.random.rand(width * height) * 100.
        saa_data = np.random.rand(width * height) + 20.
        vaa_data = np.random.rand(width * height) + 100.
        vza_data = np.random.rand(width * height) + 50.
        ozone_data = np.random.rand(width * height) + 300.
        elevation_data = np.random.rand(width * height) + 1000.
        lon_data = np.random.rand(width * height) + 120.
        lat_data = np.random.rand(width * height) + 60.

        rad_data = np.empty([21, width * height])
        for i in range(rad_data.shape[0]):
            rad_data[i] = np.random.rand(width * height) * (i + 1)

        variables = {
            'solar_zenith_angle': ['sza', 'deg', sza_data],
            'solar_azimuth_angle': ['saa', 'deg', saa_data],
            'satellite_azimuth_angle': ['vaa', 'deg', vaa_data],
            'satellite_zenith_angle': ['vza', 'deg', vza_data],
            'total_ozone': ['ozone', 'DU', ozone_data],
            'altitude': ['elevation', 'm', elevation_data]
        }

        olci_scene = xr.Dataset()

        for variable in variables:
            var_name = variables[variable][0]
            var_unit = variables[variable][1]
            var_data = variables[variable][2]
            olci_scene[var_name] = self.get_var(var_data, width, height, var_data, var_unit)

        olci_scene = olci_scene.assign_coords(
            longitude=self.get_var(lon_data, width, height, 'longitude', 'deg'),
            latitude=self.get_var(lat_data, width, height, 'latitude', 'deg'))

        coef = 1 / np.cos(np.deg2rad(olci_scene['sza'])) / 100.
        bands = [f'Oa{i:02}' for i in range(1, rad_data.shape[0] + 1)]

        toa = []
        for i in range(rad_data.shape[0]):
            toa.append(np.clip(self.get_var(rad_data[i], width, height, bands[i], 'dl') * coef, 0, 1))
        olci_scene['toa'] = xr.concat(toa, dim='band')

        print('done')

    def get_var(self, data, width, height, long_name, unit):
        data_da = xr.DataArray(
            data=data.reshape(width, height),
            dims=["x", "y"],
            attrs={'long_name': long_name,
                   'unit': unit}
        )
        return data_da.compute().stack(xy=("x", "y"))

    def test_rad_to_refl(self):
        rad = 39.03614
        sza = 82.5072
        flux = 1472.06
        refl = sice2_v21_utils.rad_to_refl(rad, sza, flux)
        self.assertAlmostEqual(0.6388, refl, 3)

    # @unittest.skip("skipping test...")
    def test_get_tif_source_product_paths(self):
        # adapt filepath before activating this test...
        tif_input_dir = PureWindowsPath("D:\\olaf\\bc\\sice2\\geus_sice2\\pySICEv21_testdata")
        tif_source_product_paths = sice2_v21_utils.get_tif_source_product_paths(str(tif_input_dir))
        self.assertIsNotNone(tif_source_product_paths)
        self.assertEqual(27, len(tif_source_product_paths))

    def test_get_valid_expression_filter_array(self):
        flagname = 'WQSF'
        valid_pixel_expr = '(WQSF.WATER and Oa10_reflectance < 0.13)   or (WQSF.LAND and  not    WQSF.CLOUD)'
        condition = sice2_v21_utils.get_condition_from_valid_pixel_expr(flagname, valid_pixel_expr,
                                                                        sice2_constants.
                                                                        OLCI_L2_IPF_BITMASK_FLAG_CODING_DICT)

        wqsf_arr = np.array([[1, 2, 18], [4, 8, 45], [18, 34, 98]])
        oa10_reflectance_arr = np.array([[0.1, 0.15, 0.12], [0.12, 0.18, 0.09], [0.03, 0.2, 0.07]])

        variables_in_expr_dict = {'WQSF': wqsf_arr, 'Oa10_reflectance': oa10_reflectance_arr}

        filter_array = sice2_v21_utils.get_valid_expression_filter_array(condition, variables_in_expr_dict, 3, 3)
        expected_arr = np.array([[False, False, True], [True, False, False], [True, False, True]])
        self.assertSequenceEqual(filter_array.tolist(), expected_arr.tolist())

        ################
        # Idepix:
        flagname = 'pixel_classif_flags'
        valid_pixel_expr = '(not pixel_classif_flags.IDEPIX_LAND and Oa10_reflectance < 0.13)   or (not pixel_classif_flags.IDEPIX_LAND and    pixel_classif_flags.IDEPIX_CLOUD)'
        condition = sice2_v21_utils.get_condition_from_valid_pixel_expr(flagname, valid_pixel_expr,
                                                                        sice2_constants.
                                                                        IDEPIX_BITMASK_FLAG_CODING_DICT)

        idepix_arr = np.array([[1, 2, 18], [4, 1024, 45], [18, 34, 98]])
        oa10_reflectance_arr = np.array([[0.1, 0.15, 0.12], [0.12, 0.18, 0.09], [0.03, 0.2, 0.07]])

        variables_in_expr_dict = {'pixel_classif_flags': idepix_arr, 'Oa10_reflectance': oa10_reflectance_arr}

        filter_array = sice2_v21_utils.get_valid_expression_filter_array(condition, variables_in_expr_dict, 3, 3)
        expected_arr = np.array([[True, True, True], [True, False, True], [True, True, True]])
        self.assertSequenceEqual(filter_array.tolist(), expected_arr.tolist())

# suite = unittest.TestLoader().loadTestsFromTestCase(TestSice2)
# unittest.TextTestRunner(verbosity=2).run(suite)
