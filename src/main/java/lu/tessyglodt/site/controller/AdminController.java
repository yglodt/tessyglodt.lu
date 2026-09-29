package lu.tessyglodt.site.controller;

import java.beans.PropertyEditorSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import lu.tessyglodt.site.data.Canton;
import lu.tessyglodt.site.data.District;
import lu.tessyglodt.site.data.Municipality;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.FacebookService;
import lu.tessyglodt.site.service.MunicipalityService;
import lu.tessyglodt.site.service.PageService;

@Controller
// @EnableAutoConfiguration
public class AdminController {

	final static Logger					logger	= LoggerFactory.getLogger(AdminController.class);

	private final PageService			pageService;

	private final MunicipalityService	municipalityService;

	private final CantonService			cantonService;

	private final DistrictService		districtService;

	private final FacebookService		facebookService;

	public AdminController(final PageService pageService, final MunicipalityService municipalityService, final CantonService cantonService, final DistrictService districtService, final FacebookService facebookService) {
		this.pageService = pageService;
		this.municipalityService = municipalityService;
		this.cantonService = cantonService;
		this.districtService = districtService;
		this.facebookService = facebookService;
	}

	@GetMapping(value = "/login")
	public String getLogin() {
		return "login";
	}

	@GetMapping(value = "/admin/pageform")
	public String getPageForm(final Model model, @RequestParam(value = "id", required = false) final String id) {

		model.addAttribute("page", ObjectUtils.isEmpty(id) ? new Page() : pageService.getPageByProperty("id", id, false));

		model.addAttribute("municipalities", municipalityService.getMunicipalities());
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());

		return "admin/pageform";
	}

	@PostMapping(value = "/admin/pageform")
	public String postPageForm(@ModelAttribute final Page page, final BindingResult result, final Model model) {

		ValidationUtils.rejectIfEmptyOrWhitespace(result, "title", "title");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "name", "name");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "latitude", "latitude");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "longitude", "longitude");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "municipality", "municipality");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "datePublished", "datePublished");

		if (result.hasErrors()) {
			logger.error("Error inserting");

			model.addAttribute("municipalities", municipalityService.getMunicipalities());
			model.addAttribute("cantons", cantonService.getCantons());
			model.addAttribute("districts", districtService.getDistricts());

			return "admin/pageform";
		} else {

			if (ObjectUtils.isEmpty(page.getId())) {
				pageService.insert(page);

				logger.debug("Inserted " + page);

				// POST to Facebook:
				// http://stackoverflow.com/questions/8487126/spring-social-facebook-publish-post-api-details

				return "redirect:/";
			} else {
				pageService.update(page);
				logger.debug("Updated " + page);
				return "redirect:/page/" + page.getName();
			}

		}
	}

	@GetMapping(value = "/admin/facebook/post-now", produces = "text/plain;charset=UTF-8")
	@ResponseBody
	public String postToFacebookNow() {
		try {
			return facebookService.postRandomPage();
		} catch (final Exception e) {
			logger.error("Posting to Facebook failed", e);
			return "Posting to Facebook failed: " + e.getMessage();
		}
	}

	@GetMapping(value = "/admin/listmcd")
	public String getListMCD(final Model model) {

		model.addAttribute("municipalities", municipalityService.getMunicipalities());
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());

		return "admin/listmcd";
	}

	@GetMapping(value = "/admin/municipalityform")
	public String getMunicipalityForm(final Model model, @RequestParam(value = "id", required = false) final Integer id) {

		model.addAttribute("municipality", ObjectUtils.isEmpty(id) ? new Municipality() : municipalityService.getMunicipality(id));
		model.addAttribute("cantons", cantonService.getCantons());

		return "admin/municipalityform";
	}

	@PostMapping(value = "/admin/municipalityform")
	public String postMunicipalityForm(@ModelAttribute final Municipality municipality, final BindingResult result, final Model model) {

		ValidationUtils.rejectIfEmptyOrWhitespace(result, "name", "name");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "canton", "canton");

		if (result.hasErrors()) {
			logger.error("Error inserting");

			model.addAttribute("cantons", cantonService.getCantons());

			return "admin/municipalityform";
		} else {

			if (ObjectUtils.isEmpty(municipality.getId())) {
				municipalityService.insert(municipality);
				return "redirect:/admin/listmcd";
			} else {
				municipalityService.update(municipality);
				return "redirect:/admin/listmcd";
			}

		}
	}

	@GetMapping(value = "/admin/cantonform")
	public String getCantonForm(final Model model, @RequestParam(value = "id", required = false) final Integer id) {

		model.addAttribute("canton", ObjectUtils.isEmpty(id) ? new Canton() : cantonService.getCanton(id));
		model.addAttribute("districts", districtService.getDistricts());

		return "admin/cantonform";
	}

	@PostMapping(value = "/admin/cantonform")
	public String postCantonForm(@ModelAttribute final Canton canton, final BindingResult result, final Model model) {

		ValidationUtils.rejectIfEmptyOrWhitespace(result, "name", "name");
		ValidationUtils.rejectIfEmptyOrWhitespace(result, "district", "district");

		if (result.hasErrors()) {
			logger.error("Error inserting");

			model.addAttribute("districts", districtService.getDistricts());

			return "admin/cantonform";
		} else {

			if (ObjectUtils.isEmpty(canton.getId())) {
				cantonService.insert(canton);
				return "redirect:/admin/listmcd";
			} else {
				cantonService.update(canton);
				return "redirect:/admin/listmcd";
			}

		}
	}

	@GetMapping(value = "/admin/districtform")
	public String getDistrictForm(final Model model, @RequestParam(value = "id", required = false) final Integer id) {
		model.addAttribute("district", ObjectUtils.isEmpty(id) ? new District() : districtService.getDistrict(id));
		return "admin/districtform";
	}

	@PostMapping(value = "/admin/districtform")
	public String postDistrictForm(@ModelAttribute final District district, final BindingResult result, final Model model) {

		ValidationUtils.rejectIfEmptyOrWhitespace(result, "name", "name");

		if (result.hasErrors()) {
			logger.error("Error inserting");

			return "admin/districtform";
		} else {

			if (ObjectUtils.isEmpty(district.getId())) {
				logger.debug("inserting");
				districtService.insert(district);
				return "redirect:/admin/listmcd";
			} else {
				logger.debug("updating");
				districtService.update(district);
				return "redirect:/admin/listmcd";
			}

		}
	}

	@InitBinder
	private void initBinder(final HttpServletRequest request, final ServletRequestDataBinder binder) throws Exception {

		binder.registerCustomEditor(Municipality.class, "municipality", new PropertyEditorSupport() {
			@Override
			public void setAsText(final String text) {
				setValue(ObjectUtils.isEmpty(text) ? null : new Municipality(Integer.valueOf(text)));
			}

			@Override
			public String getAsText() {
				final Municipality o = (Municipality) getValue();
				return (o != null) ? String.valueOf(o.getId()) : null;
			}
		});

		binder.registerCustomEditor(Canton.class, "canton", new PropertyEditorSupport() {
			@Override
			public void setAsText(final String text) {
				setValue(ObjectUtils.isEmpty(text) ? null : new Canton(Integer.valueOf(text)));
			}

			@Override
			public String getAsText() {
				final Canton o = (Canton) getValue();
				return (o != null) ? String.valueOf(o.getId()) : null;
			}
		});

		binder.registerCustomEditor(District.class, "district", new PropertyEditorSupport() {
			@Override
			public void setAsText(final String text) {
				setValue(ObjectUtils.isEmpty(text) ? null : new District(Integer.valueOf(text)));
			}

			@Override
			public String getAsText() {
				final District o = (District) getValue();
				return (o != null) ? String.valueOf(o.getId()) : null;
			}
		});

	}
}
