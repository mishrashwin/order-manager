/**
 * Phone input widget with country code selector.
 * Combines a <select> (country code) + <input> (local number) into a single E.164-formatted
 * hidden field that is submitted with the form.
 *
 * Usage:
 *   initPhoneInput('hiddenFieldId', 'countrySelectId', 'localNumberInputId')
 *
 * The hidden field stores values in E.164 format: +{countryCode}{localNumber}
 * e.g. +919876543210
 */

var PHONE_COUNTRIES = [
  { code: '+91', label: 'India (+91)', maxLen: 10 },
  { code: '+1', label: 'USA / Canada (+1)', maxLen: 10 },
  { code: '+44', label: 'UK (+44)', maxLen: 10 },
  { code: '+61', label: 'Australia (+61)', maxLen: 9 },
  { code: '+971', label: 'UAE (+971)', maxLen: 9 },
  { code: '+65', label: 'Singapore (+65)', maxLen: 8 },
  { code: '+60', label: 'Malaysia (+60)', maxLen: 9 },
  { code: '+49', label: 'Germany (+49)', maxLen: 11 },
  { code: '+33', label: 'France (+33)', maxLen: 9 },
  { code: '+86', label: 'China (+86)', maxLen: 11 },
  { code: '+81', label: 'Japan (+81)', maxLen: 10 },
  { code: '+7', label: 'Russia (+7)', maxLen: 10 },
  { code: '+55', label: 'Brazil (+55)', maxLen: 11 },
  { code: '+27', label: 'South Africa (+27)', maxLen: 9 },
  { code: '+92', label: 'Pakistan (+92)', maxLen: 10 },
  { code: '+880', label: 'Bangladesh (+880)', maxLen: 10 },
  { code: '+94', label: 'Sri Lanka (+94)', maxLen: 9 },
  { code: '+977', label: 'Nepal (+977)', maxLen: 10 },
  { code: '+966', label: 'Saudi Arabia (+966)', maxLen: 9 },
  { code: '+20', label: 'Egypt (+20)', maxLen: 10 },
  { code: '+234', label: 'Nigeria (+234)', maxLen: 10 },
  { code: '+254', label: 'Kenya (+254)', maxLen: 9 },
  { code: '+62', label: 'Indonesia (+62)', maxLen: 12 },
  { code: '+82', label: 'South Korea (+82)', maxLen: 10 },
  { code: '+52', label: 'Mexico (+52)', maxLen: 10 }
];

/**
 * Initialises one phone-input widget.
 *
 * @param {string} hiddenId - id of the hidden <input> that holds the E.164 value
 * @param {string} selectId - id of the <select> element for the country code
 * @param {string} numberId - id of the <input type="tel"> for the local number
 */
function initPhoneInput(hiddenId, selectId, numberId) {
  var hidden = document.getElementById(hiddenId);
  var select = document.getElementById(selectId);
  var numInput = document.getElementById(numberId);

  if (!hidden || !select || !numInput) {
    return;
  }

  PHONE_COUNTRIES.forEach(function(c) {
    var opt = document.createElement('option');
    opt.value = c.code;
    opt.textContent = c.label;
    select.appendChild(opt);
  });

  select.value = '+91';

  var existingValue = (hidden.value || '').trim();
  var parsed = parseExistingPhone(existingValue);
  if (parsed) {
    select.value = parsed.countryCode;
    numInput.value = parsed.localNumber;
  }

  function updateMaxLen() {
    var country = PHONE_COUNTRIES.find(function(c) {
      return c.code === select.value;
    });
    if (!country) {
      return;
    }
    numInput.maxLength = country.maxLen;
    numInput.pattern = '[0-9]{1,' + country.maxLen + '}';
    numInput.title = 'Enter up to ' + country.maxLen + ' digits';
  }

  function combine() {
    hidden.value = select.value + numInput.value;
  }

  select.addEventListener('change', function() {
    updateMaxLen();
    numInput.value = numInput.value.replace(/\D/g, '').slice(0, numInput.maxLength);
    combine();
  });

  numInput.addEventListener('input', function() {
    numInput.value = numInput.value.replace(/\D/g, '').slice(0, numInput.maxLength);
    combine();
  });

  updateMaxLen();
  combine();
}

function parseExistingPhone(value) {
  if (!value) {
    return null;
  }

  var cleaned = value.replace(/\s+/g, '');
  var normalized = cleaned.charAt(0) === '+' ? cleaned : '+' + cleaned.replace(/\D/g, '');
  var sorted = PHONE_COUNTRIES.slice().sort(function(a, b) {
    return b.code.length - a.code.length;
  });

  for (var i = 0; i < sorted.length; i++) {
    var country = sorted[i];
    if (normalized.indexOf(country.code) !== 0) {
      continue;
    }

    var local = normalized.slice(country.code.length).replace(/\D/g, '');
    if (local.length === 0) {
      continue;
    }

    if (local.length <= country.maxLen) {
      return {
        countryCode: country.code,
        localNumber: local
      };
    }
  }

  return null;
}

